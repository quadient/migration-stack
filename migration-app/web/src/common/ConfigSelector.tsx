import { useState } from "react";
import { Check, ChevronsUpDown, Copy, FileText, Plus } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Command, CommandGroup, CommandItem, CommandList, CommandSeparator } from "@/components/ui/command.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import { type CreateMode, errorMessage, selectConfig } from "@/common/configApi.ts";
import { ConfigNameDialog } from "@/dialogs/configs/ConfigNameDialog.tsx";
import type { ConfigKind } from "@/dialogs/settings/settingsTypes.ts";

type ConfigSelectorProps = {
    label: string;
    kind: ConfigKind;
    names: string[];
    templates: string[];
    active: string;
    disabled: boolean;
    onChanged: () => void;
};

export function ConfigSelector({ label, kind, names, templates, active, disabled, onChanged }: ConfigSelectorProps) {
    const [open, setOpen] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [createMode, setCreateMode] = useState<CreateMode | null>(null);

    const select = async (name: string) => {
        setOpen(false);
        if (name === active) return;
        try {
            await selectConfig(kind, name);
            setError(null);
            onChanged();
        } catch (e) {
            setError(errorMessage(e));
        }
    };

    const openNameDialog = (mode: CreateMode) => {
        setOpen(false);
        setCreateMode(mode);
    };

    const trigger = (
        <Button variant="outline" role="combobox" aria-expanded={open} disabled={disabled} className="font-normal">
            <span className="text-muted-foreground">{label}:</span>
            {active}
            <ChevronsUpDown className="opacity-50" />
        </Button>
    );

    return (
        <div className="flex items-center gap-2">
            {disabled ? (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span tabIndex={0}>{trigger}</span>
                    </TooltipTrigger>
                    <TooltipContent>A module is running</TooltipContent>
                </Tooltip>
            ) : (
                <Popover open={open} onOpenChange={setOpen}>
                    <PopoverTrigger asChild>{trigger}</PopoverTrigger>
                    <PopoverContent className="w-72 p-0">
                        <Command>
                            <CommandList>
                                <CommandGroup>
                                    {names.map((name) => (
                                        <CommandItem key={name} value={name} onSelect={() => select(name)}>
                                            {name}
                                            <Check
                                                className={`ml-auto ${name === active ? "opacity-100" : "opacity-0"}`}
                                            />
                                        </CommandItem>
                                    ))}
                                </CommandGroup>
                                <CommandSeparator />
                                <CommandGroup>
                                    <CommandItem value="__new__" onSelect={() => openNameDialog({ type: "new" })}>
                                        <Plus />
                                        New…
                                    </CommandItem>
                                    {templates.length > 0 && (
                                        <CommandItem
                                            value="__template__"
                                            onSelect={() =>
                                                openNameDialog({ type: "template", template: templates[0] })
                                            }
                                        >
                                            <FileText />
                                            New from template…
                                        </CommandItem>
                                    )}
                                    <CommandItem
                                        value="__clone__"
                                        onSelect={() => openNameDialog({ type: "clone", from: active })}
                                    >
                                        <Copy />
                                        Clone '{active}'…
                                    </CommandItem>
                                </CommandGroup>
                            </CommandList>
                        </Command>
                    </PopoverContent>
                </Popover>
            )}
            {error && <span className="text-sm text-destructive">{error}</span>}
            {createMode && (
                <ConfigNameDialog
                    kind={kind}
                    mode={createMode}
                    templates={templates}
                    onClose={() => setCreateMode(null)}
                    onCreated={() => {
                        setCreateMode(null);
                        setError(null);
                        onChanged();
                    }}
                />
            )}
        </div>
    );
}
