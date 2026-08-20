#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $EUID -ne 0 ]]; then
    echo "bootstrap-ubuntu.sh must run as root" >&2
    exit 1
fi
if [[ $# -ne 1 || ! $1 =~ ^[A-Za-z0-9.-]+$ ]]; then
    echo "usage: bootstrap-ubuntu.sh <api-domain>" >&2
    exit 2
fi

domain=$1
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(realpath "$script_dir/../..")

apt update
DEBIAN_FRONTEND=noninteractive apt install -y openjdk-21-jdk maven nginx curl ca-certificates openssl

if ! id skillport >/dev/null 2>&1; then
    useradd --system --home /var/lib/skillport --shell /usr/sbin/nologin skillport
fi

install -d -m 0755 -o root -g skillport /opt/skillport /opt/skillport/releases
install -d -m 0750 -o skillport -g skillport /var/lib/skillport /var/lib/skillport/skills \
    /var/lib/skillport/tmp
install -d -m 0750 -o root -g skillport /etc/skillport

install -m 0755 "$repo_root/deploy/scripts/skillport-deploy" /usr/local/sbin/skillport-deploy
install -m 0755 "$repo_root/deploy/scripts/skillport-rollback" /usr/local/sbin/skillport-rollback
install -m 0644 "$repo_root/deploy/systemd/skillport.service" /etc/systemd/system/skillport.service
install -m 0440 "$repo_root/deploy/sudoers/jenkins-skillport" /etc/sudoers.d/jenkins-skillport
visudo -cf /etc/sudoers.d/jenkins-skillport

if [[ ! -e /etc/skillport/skillport.env ]]; then
    install -m 0640 -o root -g skillport "$repo_root/deploy/config/skillport.env.example" \
        /etc/skillport/skillport.env
fi
if [[ ! -e /etc/skillport/mysql.env ]]; then
    install -m 0600 -o root -g root "$repo_root/deploy/config/mysql.env.example" /etc/skillport/mysql.env
fi

install -m 0644 "$repo_root/deploy/docker-compose.prod.yml" /opt/skillport/docker-compose.prod.yml
sed "s/api\.example\.com/$domain/g" "$repo_root/deploy/nginx/skillport.conf" \
    > /etc/nginx/conf.d/skillport.conf

systemctl daemon-reload
systemctl enable skillport.service
nginx -t
systemctl reload nginx

echo "Bootstrap complete. Edit /etc/skillport/*.env before starting MySQL or running Jenkins deployment."
