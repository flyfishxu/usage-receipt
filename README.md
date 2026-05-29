# Usage Receipt

It collects usage data from Codex and Claude Code hooks, calculates USD cost from a local model pricing table, and prints a shopping-receipt-style ticket through an ESC/POS network printer over TCP `IP:9100`.

It's still Working in progress~

| ![UsageReceipt UI](https://cloudflareimg.cdn.sn/i/6a190accbab2d_1780026060.webp) | ![Receipt](https://cloudflareimg.cdn.sn/i/6a190d218115b_1780026657.webp) |
|:---:|:---:|
## Features

- Compose Desktop UI with printer, hook, session, and receipt preview panels.
- Codex and Claude Code hook installation with user config backup.
- Local usage history stored under `~/.usageReceipt`.
- Manual print for one turn or an aggregated full session.
- ESC/POS rendering with 58mm and 80mm paper widths.
- OpenAI and Anthropic logo rasterization from SVG for receipt headers.
- Single network printer support now, with data model fields reserved for multiple printers later.
- 
## Run

```bash
./gradlew :desktopApp:run
```

The UI lets you configure:

- printer IP address
- printer port, default `9100`
- receipt width, `58mm` or `80mm`
- Codex and Claude Code automatic print hooks

## Current Scope

This first version focuses on local macOS desktop usage, one configured network printer, USD pricing, and automatic printing after each completed turn. Cloud sync, multi-printer routing, online price updates, and currency conversion are intentionally left for later.

## License

MIT
