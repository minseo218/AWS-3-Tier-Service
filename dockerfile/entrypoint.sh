#!/bin/sh

# Fluentd 실행
exec td-agent &

# Nginx 실행
exec nginx -g "daemon off;"

