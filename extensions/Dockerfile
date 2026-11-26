FROM python:3.12-slim
WORKDIR /app
COPY gateway.py ./
COPY hc_plugins/ ./hc_plugins/
RUN useradd --system --uid 10001 --home /app hcplugins \
    && mkdir -p /data \
    && chown -R hcplugins:hcplugins /data
USER hcplugins
ENV PYTHONDONTWRITEBYTECODE=1 GATEWAY_HOST=0.0.0.0 GATEWAY_PORT=8788 GATEWAY_DB=/data/summary.sqlite3
EXPOSE 8788
CMD ["python", "gateway.py"]
