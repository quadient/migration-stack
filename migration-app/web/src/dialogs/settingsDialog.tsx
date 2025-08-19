import {useEffect, useState} from "react";
import {
    Dialog,
    DialogContent,
    DialogTrigger,
    DialogHeader,
    DialogTitle,
    DialogDescription
} from "@/components/ui/dialog.tsx"
import type {ReactNode} from "react";

type SettingsDialogProps = {
    trigger: ReactNode;
};

export default function SettingsDialog({trigger}: SettingsDialogProps) {
    const [settings, setSettings] = useState<string | null>(null);

    useEffect(() => {
        fetchSettings()
            .then(setSettings)
            .catch(() => setSettings(null));
    }, []);

    return (
        <Dialog modal>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent className="bg-white border-0">
                <DialogHeader>
                    <DialogTitle>Settings</DialogTitle>
                    <DialogDescription className="text-gray-500">
                        Configure project settings and connection to services
                    </DialogDescription>
                </DialogHeader>
                {settings && (<div className="mt-4 text-sm">{settings}</div>)}
            </DialogContent>
        </Dialog>
    );
}

async function fetchSettings(): Promise<string> {
    const response = await fetch("/api/settings");
    if (!response.ok) {
        throw new Error("Failed to fetch settings");
    }
    return response.text();
}