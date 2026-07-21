import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        money_movement: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '1m', target: 10 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

function registerAndLogin(email) {
    const registerRes = http.post(
        `${BASE_URL}/api/v1/users/register`,
        JSON.stringify({
            email,
            phone: '+1555' + Math.floor(1000000 + Math.random() * 8999999),
            fullName: 'Load Test User',
            password: 'LoadTest123!',
        }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    check(registerRes, { 'register succeeded': (r) => r.status === 201 });

    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email, password: 'LoadTest123!' }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    check(loginRes, { 'login succeeded': (r) => r.status === 200 });
    return loginRes.json('accessToken');
}

export function setup() {
    const email = `k6-${Date.now()}@bank-platform.test`;
    const accessToken = registerAndLogin(email);
    return { accessToken };
}

export default function (data) {
    const authHeaders = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.accessToken}`,
    };

    const openAccountRes = http.post(
        `${BASE_URL}/api/v1/accounts`,
        JSON.stringify({ type: 'CURRENT', currency: 'USD' }),
        { headers: authHeaders },
    );
    const opened = check(openAccountRes, { 'account opened': (r) => r.status === 201 });
    if (!opened) {
        sleep(1);
        return;
    }
    const accountId = openAccountRes.json('id');

    const depositRes = http.post(
        `${BASE_URL}/api/v1/transactions/deposits`,
        JSON.stringify({ accountId, amount: 100.0, currency: 'USD' }),
        { headers: { ...authHeaders, 'Idempotency-Key': uuidv4() } },
    );
    const deposited = check(depositRes, { 'deposit accepted': (r) => r.status === 202 });

    if (deposited) {
        const transactionId = depositRes.json('transactionId');
        sleep(0.5);
        const statusRes = http.get(`${BASE_URL}/api/v1/transactions/${transactionId}`, { headers: authHeaders });
        check(statusRes, { 'transaction status readable': (r) => r.status === 200 });
    }

    sleep(1);
}
