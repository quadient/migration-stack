import { useState } from "react";
import { Check, ChevronsUpDown } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { Command, CommandGroup, CommandItem, CommandList } from "@/components/ui/command.tsx";

type SettingsComboboxProps = {
    currentValue?: string;
    onChange: (value: string) => void;
    options: string[] | undefined;
};

export function SettingsCombobox({ currentValue, onChange, options }: SettingsComboboxProps) {
    const [open, setOpen] = useState(false);

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    className="w-full justify-between font-normal"
                >
                    {currentValue ?? "Select value"}
                    <ChevronsUpDown className="opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-100 p-0">
                {options && (
                    <Command>
                        <CommandList>
                            <CommandGroup>
                                {options.map((option) => (
                                    <CommandItem
                                        key={option}
                                        value={option}
                                        onSelect={(newValue) => {
                                            if (newValue !== currentValue) {
                                                onChange(newValue);
                                            }
                                            setOpen(false);
                                        }}
                                    >
                                        {option}
                                        <Check
                                            className={`ml-auto ${currentValue === option ? "opacity-100" : "opacity-0"}`}
                                        />
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        </CommandList>
                    </Command>
                )}
            </PopoverContent>
        </Popover>
    );
}
