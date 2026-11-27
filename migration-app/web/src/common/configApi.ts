import type { ConfigKind, Settings } from "@/dialogs/settings/settingsTypes.ts";

export async function selectConfig(kind: ConfigKind, name: string): Promise<void> {
    const body = kind === "project" ? { projectConfig: name } : { migrationConfig: name };
    await sendJson("PUT", "/api/configs/active", body);
}

export type CreateMode = { type: "new" } | { type: "clone"; from: string } | { type: "template"; template: string };

export async function createConfig(kind: ConfigKind, name: string, mode: CreateMode): Promise<void> {
    const cloneFrom = mode.type === "clone" ? mode.from : null;
    const template = mode.type === "template" ? mode.template : null;
    await sendJson("POST", `/api/configs/${kind}`, { name, cloneFrom, template });
}

export async function setSourceFormat(sourceFormat: string): Promise<void> {
    await sendJson("PUT", "/api/configs/source-format", { sourceFormat });
}

export async function fetchSettings(): Promise<Settings> {
    const response = await fetch("/api/settings");
    if (!response.ok) {
        throw new Error((await response.text()) || `Request failed with status ${response.status}`);
    }
    return (await response.json()) as Settings;
}

export function errorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}

async function sendJson(method: string, url: string, body: unknown): Promise<void> {
    const response = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    if (!response.ok) {
        throw new Error((await response.text()) || `Request failed with status ${response.status}`);
    }
}
