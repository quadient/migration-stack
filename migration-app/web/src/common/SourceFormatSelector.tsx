import { useState } from "react";
import { Check, ChevronsUpDown } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";
import { errorMessage, setSourceFormat } from "@/common/configApi.ts";

type SourceFormatSelectorProps = {
    formats: string[];
    active: string | undefined;
    onChanged: () => void;
};

export function SourceFormatSelector({ formats, active, onChanged }: SourceFormatSelectorProps) {
    const [open, setOpen] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const select = async (format: string) => {
        setOpen(false);
        if (format === active) return;
        try {
            await setSourceFormat(format);
            setError(null);
            onChanged();
        } catch (e) {
            setError(errorMessage(e));
        }
    };

    return (
        <div className="flex items-center gap-2">
            <Popover open={open} onOpenChange={setOpen}>
                <PopoverTrigger asChild>
                    <Button
                        variant="outline"
                        role="combobox"
                        aria-expanded={open}
                        disabled={formats.length === 0}
                        className="font-normal"
                    >
                        <span className="text-muted-foreground">Source:</span>
                        {active ?? "not set"}
                        <ChevronsUpDown className="opacity-50" />
                    </Button>
                </PopoverTrigger>
                <PopoverContent className="w-72 p-0">
                    <Command>
                        <CommandList>
                            <CommandGroup>
                                {formats.map((format) => (
                                    <CommandItem key={format} value={format} onSelect={() => select(format)}>
                                        {format}
                                        <Check
                                            className={`ml-auto ${format === active ? "opacity-100" : "opacity-0"}`}
                                        />
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        </CommandList>
                    </Command>
                </PopoverContent>
            </Popover>
            {error && <span className="text-sm text-destructive">{error}</span>}
        </div>
    );
}
