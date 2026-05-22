#!/usr/bin/env bash
# Caddy reverse proxy for Ollama with TLS (self-signed)
# Usage: ./ollama-proxy.sh [port] [ollama_port]

PORT="${1:-8443}"
OLLAMA_PORT="${2:-11434}"
IP=$(hostname -I | awk '{print $1}')
CADDYFILE=$(mktemp)

cat > "$CADDYFILE" <<EOF
{
	auto_https disable_redirects
}

https://${IP}:${PORT} {
	tls internal
	reverse_proxy localhost:${OLLAMA_PORT}
}
EOF

echo "Ollama TLS proxy: https://${IP}:${PORT}/v1"
echo "Model list:       https://${IP}:${PORT}/v1/models"
echo "Ollama backend:   http://localhost:${OLLAMA_PORT}"
echo ""
echo "For AAPdroid LLM config:"
echo "  URL:   https://${IP}:${PORT}/v1"
echo "  Cert:  Enable 'Accept self-signed certificate'"
echo ""

caddy run --config "$CADDYFILE" --adapter caddyfile
