#!/bin/sh

# Fluentd ����
exec td-agent &

# Nginx ����
exec nginx -g "daemon off;"

