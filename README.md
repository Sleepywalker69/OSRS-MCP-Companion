# OSRS MCP Companion — RuneLite Plugin

A RuneLite plugin that saves your player data locally as JSON files, enabling
AI assistants to give personalized Old School RuneScape advice through the
Model Context Protocol (MCP).

## What It Does

Periodically saves a snapshot of your player data to local JSON files
in `~/.runelite/osrs-companion/`. This data can then be read by the companion MCP
server to give AI assistants (like Claude) context about your account.

**Data stays on your machine. Nothing is sent over the network.**

## What Gets Synced

| Data                | Trigger                          |
|---------------------|----------------------------------|
| Skill levels & XP   | On login + when stats change     |
| Bank contents       | When you open your bank          |
| Inventory           | On item changes                  |
| Equipment           | On equipment changes             |
| Quest status        | Polled every ~18 seconds         |
| Achievement Diaries | Polled every ~18 seconds         |
| Combat Achievements | Polled every ~18 seconds         |

All data categories can be individually toggled in the plugin settings.

## Configuration

- **Save Interval**: How often to write to disk (default: 60 seconds, minimum: 30)
- **Data Toggles**: Enable/disable syncing for each data category

## Where Data Is Saved

```
~/.runelite/osrs-companion/{username}.json
```

Each logged-in character gets its own JSON file, updated periodically and on logout.

## Using with an MCP Server

This plugin is one half of the system. To connect your player data to an AI
assistant, you also need the companion MCP server:

**[osrs-companion](https://www.npmjs.com/package/osrs-companion)** — A
local MCP server that reads the JSON files and exposes them as tools, plus wiki
search, page summaries, and GE price lookups.

### Quick Setup (Claude Code / Claude Desktop)

Add to your MCP configuration:

```json
{
  "mcpServers": {
    "osrs-companion": {
      "command": "npx",
      "args": ["-y", "osrs-companion"]
    }
  }
}
```

## Privacy

- All data is saved to local files only
- No network requests are made by this plugin
- No data is sent to any server, API, or third party
- You control exactly what gets synced via the config panel

## License

BSD 2-Clause "Simplified" License. See [LICENSE](LICENSE).
