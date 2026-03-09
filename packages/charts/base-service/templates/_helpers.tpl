{{/*
CloudInfraMap Base Service - Template Helpers
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "base-service.name" -}}
{{- default .Chart.Name .Values.unitName | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "base-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- .Values.unitName | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "base-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "base-service.labels" -}}
helm.sh/chart: {{ include "base-service.chart" . }}
{{ include "base-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: cloudinframap
cloudinframap.com/unit-id: {{ .Values.unitId | quote }}
cloudinframap.com/domain: {{ .Values.domain | quote }}
cloudinframap.com/region: {{ .Values.region | quote }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "base-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "base-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "base-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "base-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Dapr annotations
*/}}
{{- define "base-service.daprAnnotations" -}}
{{- if .Values.dapr.enabled }}
dapr.io/enabled: "true"
dapr.io/app-id: {{ .Values.dapr.appId | quote }}
dapr.io/app-port: {{ .Values.dapr.appPort | quote }}
dapr.io/app-protocol: {{ .Values.dapr.protocol | quote }}
{{- if .Values.debug.enabled }}
dapr.io/log-level: {{ .Values.debug.daprLogLevel | default "debug" | quote }}
{{- else }}
dapr.io/log-level: {{ .Values.dapr.logLevel | quote }}
{{- end }}
dapr.io/config: {{ .Values.dapr.config | quote }}
dapr.io/sidecar-cpu-limit: {{ .Values.dapr.cpuLimit | quote }}
dapr.io/sidecar-memory-limit: {{ .Values.dapr.memoryLimit | quote }}
dapr.io/sidecar-cpu-request: {{ .Values.dapr.cpuRequest | quote }}
dapr.io/sidecar-memory-request: {{ .Values.dapr.memoryRequest | quote }}
{{- if hasKey .Values.dapr "enableAppHealthCheck" }}
dapr.io/enable-app-health-check: {{ .Values.dapr.enableAppHealthCheck | quote }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Init container waiting for a dependency
*/}}
{{- define "unit.initContainer.waitFor" -}}
- name: wait-for-{{ .targetService }}
  image: {{ .image | default "curlimages/curl:8.5.0" }}
  command:
    - sh
    - -c
    - |
      echo "Waiting for {{ .targetService }}..."
      max_attempts={{ .maxAttempts | default 60 }}
      attempt=0
      while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://{{ .targetService }}:{{ .targetPort | default 8080 }}/ready; then
          echo "{{ .targetService }} is ready!"
          exit 0
        fi
        echo "Attempt $attempt/$max_attempts"
        sleep {{ .sleepSeconds | default 2 }}
        attempt=$((attempt + 1))
      done
      echo "ERROR: {{ .targetService }} not ready"
      exit 1
{{- end }}

{{/*
Standard health probes
*/}}
{{- define "unit.probes" -}}
startupProbe:
  httpGet:
    path: {{ .Values.healthCheck.health.path }}
    port: {{ .Values.healthCheck.health.port }}
  initialDelaySeconds: {{ .Values.probes.startup.initialDelaySeconds }}
  periodSeconds: {{ .Values.probes.startup.periodSeconds }}
  failureThreshold: {{ .Values.probes.startup.failureThreshold }}
readinessProbe:
  httpGet:
    path: {{ .Values.healthCheck.ready.path }}
    port: {{ .Values.healthCheck.ready.port }}
  initialDelaySeconds: {{ .Values.probes.readiness.initialDelaySeconds }}
  periodSeconds: {{ .Values.probes.readiness.periodSeconds }}
  failureThreshold: {{ .Values.probes.readiness.failureThreshold }}
livenessProbe:
  httpGet:
    path: {{ .Values.healthCheck.live.path }}
    port: {{ .Values.healthCheck.live.port }}
  initialDelaySeconds: {{ .Values.probes.liveness.initialDelaySeconds }}
  periodSeconds: {{ .Values.probes.liveness.periodSeconds }}
  failureThreshold: {{ .Values.probes.liveness.failureThreshold }}
{{- end }}
