import { useState } from "react";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Input } from "@/components/ui/input.tsx";
import { cn } from "@/lib/utils.ts";
import { sendInputAnswer, useInputRequest } from "@/common/inputRequests.ts";
import type { InputRequest } from "@/types/inputRequest.ts";

export default function InputRequestDialog() {
    const request = useInputRequest();

    return (
        <Dialog modal open={!!request} onOpenChange={(open) => !open && request && sendInputAnswer(request, null)}>
            {request && <InputRequestContent key={request.questionId} request={request} />}
        </Dialog>
    );
}

function InputRequestContent({ request }: { request: InputRequest }) {
    const [value, setValue] = useState(request.kind === "chooseOrType" ? (request.default ?? "") : "");
    const answer = (answer: string | null) => sendInputAnswer(request, answer);

    return (
        <DialogContent onInteractOutside={(event) => event.preventDefault()}>
            <DialogHeader>
                <DialogTitle>Module needs your input</DialogTitle>
                <DialogDescription className="break-words whitespace-pre-wrap">{request.message}</DialogDescription>
            </DialogHeader>

            {request.kind !== "confirm" && (
                <OptionList options={request.options} selected={value} onSelect={setValue} onConfirm={answer} />
            )}
            {request.kind === "chooseOrType" && (
                <Input value={value} placeholder={request.default ?? ""} onChange={(e) => setValue(e.target.value)} />
            )}

            <DialogFooter>
                <Button variant="outline" onClick={() => answer(null)}>
                    Cancel
                </Button>
                {request.kind === "confirm" ? (
                    <>
                        <Button variant="outline" onClick={() => answer("no")}>
                            No
                        </Button>
                        <Button onClick={() => answer("yes")}>Yes</Button>
                    </>
                ) : (
                    <Button disabled={request.kind === "choose" && !value} onClick={() => answer(value)}>
                        OK
                    </Button>
                )}
            </DialogFooter>
        </DialogContent>
    );
}

type OptionListProps = {
    options: string[];
    selected: string;
    onSelect: (option: string) => void;
    onConfirm: (option: string) => void;
};

function OptionList({ options, selected, onSelect, onConfirm }: OptionListProps) {
    if (options.length === 0) {
        return null;
    }

    return (
        <div role="radiogroup" className="max-h-64 divide-y overflow-y-auto rounded-md border">
            {options.map((option) => {
                const isSelected = option === selected;
                return (
                    <button
                        key={option}
                        type="button"
                        role="radio"
                        aria-checked={isSelected}
                        className={cn(
                            "flex w-full items-center gap-2 px-3 py-2 text-left text-sm transition-colors outline-none hover:bg-accent/50 focus-visible:bg-accent/50",
                            isSelected && "bg-accent font-medium text-accent-foreground hover:bg-accent",
                        )}
                        onClick={() => onSelect(option)}
                        onDoubleClick={() => onConfirm(option)}
                    >
                        <Check className={cn("size-4 shrink-0 text-primary", !isSelected && "invisible")} />
                        <span className="break-all">{option}</span>
                    </button>
                );
            })}
        </div>
    );
}
