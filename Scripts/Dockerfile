FROM python:3.8-alpine3.15

# Install dependencies
COPY requirements.txt /tmp/
RUN apk --update add gcc musl-dev libffi-dev libxml2-dev libxslt-dev openssl-dev && \
	cd /tmp/ && pip install -r requirements.txt && pip install pylint && \
	apk del gcc musl-dev libffi-dev openssl-dev && rm -rf /var/cache/apk/* /tmp /root/.cache


