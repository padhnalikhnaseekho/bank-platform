{{- define "bank-platform.image" -}}
{{ .root.Values.global.imageRegistry }}/{{ .svc.name }}:{{ .root.Values.global.imageTag }}
{{- end -}}
