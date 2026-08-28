// Minimal allowlist HTTP CONNECT proxy used to sandbox outbound traffic
// for GitHub repository clone operations. Only CONNECT (HTTPS tunneling)
// to hosts listed in ALLOWED_HOSTS is permitted; everything else is denied.
const net = require('net');
const http = require('http');

const ALLOWED_HOSTS = (process.env.ALLOWED_HOSTS || '')
  .split(',')
  .map((h) => h.trim().toLowerCase())
  .filter(Boolean);

function isAllowed(host) {
  const h = host.toLowerCase();
  return ALLOWED_HOSTS.some((allowed) => {
    if (allowed.startsWith('*.')) {
      return h.endsWith(allowed.slice(1));
    }
    return h === allowed;
  });
}

const server = http.createServer((req, res) => {
  res.writeHead(405, { 'Content-Type': 'text/plain' });
  res.end('This sandbox proxy only supports CONNECT (HTTPS) tunneling.\n');
});

server.on('connect', (req, clientSocket, head) => {
  const [host, portStr] = req.url.split(':');
  const port = parseInt(portStr, 10) || 443;

  if (!host || !isAllowed(host)) {
    console.log(`[sandbox-proxy] DENY ${host}:${port}`);
    clientSocket.end('HTTP/1.1 403 Forbidden\r\n\r\n');
    return;
  }

  const serverSocket = net.connect(port, host, () => {
    clientSocket.write('HTTP/1.1 200 Connection Established\r\n\r\n');
    serverSocket.write(head);
    serverSocket.pipe(clientSocket);
    clientSocket.pipe(serverSocket);
  });

  serverSocket.on('error', () => clientSocket.destroy());
  clientSocket.on('error', () => serverSocket.destroy());
  console.log(`[sandbox-proxy] ALLOW ${host}:${port}`);
});

const PORT = process.env.PROXY_PORT || 3128;
server.listen(PORT, () => {
  console.log(`[sandbox-proxy] listening on :${PORT}`);
  console.log(`[sandbox-proxy] allowed hosts: ${ALLOWED_HOSTS.join(', ') || '(none)'}`);
});
