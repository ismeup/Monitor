#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

INSTALL_DIR="/opt/ismeup-monitor"
JAR_NAME="Monitor.jar"
GITHUB_REPO="ismeup/Monitor"
SERVICE_NAME="ismeup-monitor"
WORKDIR=$(mktemp -d)

cleanup() { rm -rf "$WORKDIR"; }
trap cleanup EXIT

info()    { echo -e "${BLUE}==>${NC} $*"; }
ok()      { echo -e "${GREEN}==>${NC} $*"; }
warn()    { echo -e "${YELLOW}==>${NC} $*"; }
die()     { echo -e "${RED}error:${NC} $*" >&2; exit 1; }
ask()     { echo -en "${BOLD}$*${NC}"; }

# ── Java ────────────────────────────────────────────────────────────────────
JAVA_BIN=$(command -v java 2>/dev/null) \
    || die "Java 8+ is required but not found. Please install Java and try again."

# ── Latest version via GitHub API ───────────────────────────────────────────
info "Fetching latest version from GitHub..."
API_RESPONSE=$(curl -fsSL "https://api.github.com/repos/${GITHUB_REPO}/releases/latest") \
    || die "Failed to reach GitHub API. Check your internet connection."

VERSION=$(echo "$API_RESPONSE" | grep '"tag_name"' \
    | sed 's/.*"tag_name": *"v\?\([^"]*\)".*/\1/')

[[ -n "$VERSION" ]] || die "Could not parse latest version from GitHub API response."
info "Latest version: ${BOLD}${VERSION}${NC}"

JAR_URL="https://github.com/${GITHUB_REPO}/releases/download/v${VERSION}/Monitor-${VERSION}-jar-with-dependencies.jar"

# Reconnect stdin to the terminal — required when script is piped through curl
exec < /dev/tty

# ── Update mode: existing installation detected ─────────────────────────────
if [[ -f "${INSTALL_DIR}/${JAR_NAME}" ]]; then
    warn "Existing installation found in ${INSTALL_DIR}"
    ask "Update JAR to v${VERSION}? config.json will not be touched [Y/n]: "
    read -r ans
    [[ "${ans,,}" == "n" ]] && { info "Nothing changed. Exiting."; exit 0; }

    info "Downloading Monitor v${VERSION}..."
    curl -fsSL --progress-bar "$JAR_URL" -o "${WORKDIR}/${JAR_NAME}" \
        || die "Download failed."

    sudo cp "${WORKDIR}/${JAR_NAME}" "${INSTALL_DIR}/${JAR_NAME}"
    ok "JAR updated to v${VERSION}."

    if systemctl is-active --quiet "$SERVICE_NAME" 2>/dev/null; then
        info "Restarting ${SERVICE_NAME}..."
        sudo systemctl restart "$SERVICE_NAME"
        ok "Service restarted."
    fi

    ok "Done."
    exit 0
fi

# ── Fresh install ────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}isMeUp Monitor — Installation${NC}"
echo "-------------------------------------------"

# Download to temp dir (no sudo needed yet)
info "Downloading Monitor v${VERSION}..."
curl -fsSL --progress-bar "$JAR_URL" -o "${WORKDIR}/${JAR_NAME}" \
    || die "Download failed. URL: ${JAR_URL}"
ok "Download complete."

# Run --setup as current user; config.json is created alongside the JAR
echo ""
info "Starting configuration wizard..."
echo "-------------------------------------------"
cd "$WORKDIR"
java -jar "$JAR_NAME" --setup
echo "-------------------------------------------"

[[ -f "${WORKDIR}/config.json" ]] \
    || die "--setup did not produce config.json. Configuration may have failed."

# Move to /opt (first sudo touch)
info "Installing to ${INSTALL_DIR}..."
sudo mkdir -p "$INSTALL_DIR"
sudo cp "${WORKDIR}/${JAR_NAME}"   "${INSTALL_DIR}/${JAR_NAME}"
sudo cp "${WORKDIR}/config.json"   "${INSTALL_DIR}/config.json"
ok "Installed to ${INSTALL_DIR}"

# ── Systemd ──────────────────────────────────────────────────────────────────
echo ""
ask "Install as systemd service? [Y/n]: "
read -r ans_service
if [[ "${ans_service,,}" == "n" ]]; then
    ok "Done. Start manually with:"
    echo "    java -jar ${INSTALL_DIR}/${JAR_NAME}"
    exit 0
fi

ask "Run service as user [ismeup]: "
read -r SVC_USER
SVC_USER="${SVC_USER:-ismeup}"

if ! id "$SVC_USER" &>/dev/null; then
    info "Creating system user '${SVC_USER}'..."
    sudo useradd -r -s /sbin/nologin -d "$INSTALL_DIR" -c "isMeUp Monitor" "$SVC_USER"
    ok "User '${SVC_USER}' created."
fi

sudo chown -R "${SVC_USER}:${SVC_USER}" "$INSTALL_DIR"

info "Writing /etc/systemd/system/${SERVICE_NAME}.service..."
sudo tee "/etc/systemd/system/${SERVICE_NAME}.service" >/dev/null <<EOF
[Unit]
Description=isMeUp Monitor
After=network.target

[Service]
Type=simple
User=${SVC_USER}
WorkingDirectory=${INSTALL_DIR}
ExecStart=${JAVA_BIN} -jar ${INSTALL_DIR}/${JAR_NAME}
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now "$SERVICE_NAME"

echo ""
ok "Service '${SERVICE_NAME}' enabled and started."
ok "Installation complete."
echo ""
echo "  Manage with:"
echo "    sudo systemctl status  ${SERVICE_NAME}"
echo "    sudo systemctl restart ${SERVICE_NAME}"
echo "    sudo systemctl stop    ${SERVICE_NAME}"
