import { type FormEvent, useState } from "react";
import { Button } from "@/components/ui/button.tsx";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Label } from "@/components/ui/label.tsx";
import { createConfig, type CreateMode, errorMessage, selectConfig } from "@/common/configApi.ts";
import { SettingsCombobox } from "@/dialogs/settings/SettingsCombobox.tsx";
import type { ConfigKind } from "@/dialogs/settings/settingsTypes.ts";

type ConfigNameDialogProps = {
    kind: ConfigKind;
    mode: CreateMode;
    templates: string[];
    onClose: () => void;
    onCreated: () => void;
};

export function ConfigNameDialog({ kind, mode: initialMode, templates, onClose, onCreated }: ConfigNameDialogProps) {
    const [name, setName] = useState("");
    const [mode, setMode] = useState(initialMode);
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    const kindLabel = kind === "project" ? "project config" : "migration config";
    const titles: Record<CreateMode["type"], string> = {
        new: `New ${kindLabel}`,
        clone: `Clone ${kindLabel} '${mode.type === "clone" ? mode.from : ""}'`,
        template: `New ${kindLabel} from template`,
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setBusy(true);
        try {
            await createConfig(kind, name.trim(), mode);
            await selectConfig(kind, name.trim());
            onCreated();
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent>
                <form onSubmit={submit} className="grid gap-4">
                    <DialogHeader>
                        <DialogTitle>{titles[mode.type]}</DialogTitle>
                    </DialogHeader>
                    {mode.type === "template" && (
                        <div className="grid gap-3">
                            <Label>Template</Label>
                            <SettingsCombobox
                                currentValue={mode.template}
                                onChange={(template) => setMode({ type: "template", template })}
                                options={templates}
                            />
                        </div>
                    )}
                    <div className="grid gap-3">
                        <Label htmlFor="config-name">Name</Label>
                        <Input id="config-name" autoFocus value={name} onChange={(e) => setName(e.target.value)} />
                    </div>
                    {error && <p className="text-sm text-destructive">{error}</p>}
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={onClose}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={busy || name.trim() === ""}>
                            Create
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
