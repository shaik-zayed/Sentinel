**Commands**

1. This forwards port to connect to remote docker over ssh
- `ssh -NL 2375:/var/run/docker.sock user@ip`
- Securely tunnels your local port 2375 to the remote Docker socket so your app can control the remote Docker as if it were local.


1. Creating individual docker images
- `docker build -t eureka-server --build-arg SERVICE_NAME=eureka-server .`

