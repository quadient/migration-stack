import { useEffect, useState } from "react";
import type { JSX } from "react";
import {
    Dialog,
    DialogContent,
    DialogTrigger,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog.tsx";
import type { ReactNode } from "react";

type SettingsDialogProps = {
    trigger: ReactNode;
};

type SettingsNode = {
    key: string;
    value?: string | number | boolean | null;
    children?: SettingsNode[];
};

export default function SettingsDialog({ trigger }: SettingsDialogProps) {
    const [settingsTree, setSettingsTree] = useState<SettingsNode[] | null>(null);

    useEffect(() => {
        fetchSettings()
            .then((json) => {
                console.log("Fetched settings:", json);
                setSettingsTree(buildSettingsTree(json));
            })
            .catch(() => {
                console.error("Failed to fetch or parse settings");
                setSettingsTree(null);
            });
    }, []);

    return (
        <Dialog modal>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-gray-500">
                        Configure project settings and connection to services
                    </DialogDescription>
                </DialogHeader>
                {settingsTree && <div>{renderSettingsTree(settingsTree)}</div>}
            </DialogContent>
        </Dialog>
    );
}

function renderSettingsTree(nodes: SettingsNode[], level = 1): ReactNode {
    return nodes.map((node) => {
        if (node.children && node.children.length > 0) {
            const HeadingTag = `h${Math.min(level, 6)}` as keyof JSX.IntrinsicElements;
            return (
                <div key={node.key} style={{ marginLeft: (level - 1) * 16, marginTop: 8 }}>
                    <HeadingTag>{node.key}</HeadingTag>
                    {renderSettingsTree(node.children, level + 1)}
                </div>
            );
        } else {
            let inputType = "text";
            if (typeof node.value === "number") inputType = "number";
            return (
                <div key={node.key} style={{ marginLeft: (level - 1) * 16, marginTop: 4 }}>
                    <label style={{ marginRight: 8 }}>
                        {node.key}:
                        <input
                            type={inputType}
                            value={typeof node.value === "boolean" ? String(node.value) : (node.value ?? "")}
                            style={{ marginLeft: 8 }}
                            readOnly
                        />
                    </label>
                </div>
            );
        }
    });
}

async function fetchSettings(): Promise<any> {
    const response = await fetch("/api/settings");
    if (!response.ok) {
        throw new Error("Failed to fetch settings");
    }
    return await response.json();
}

function buildSettingsTree(obj: any): SettingsNode[] {
    console.log("Building settings tree from object:", obj);
    return Object.entries(obj).map(([key, value]) => {
        if (value !== null && typeof value === "object" && !Array.isArray(value)) {
            return {
                key: key,
                children: buildSettingsTree(value),
            } as SettingsNode;
        } else {
            return {
                key: key,
                value: value as string | number | boolean | null | undefined,
            } as SettingsNode;
        }
    });
}
