import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog.tsx";
import { ScrollArea } from "@/components/ui/scroll-area.tsx";
import { type ReactNode, useState } from "react";
import type { RunResult } from "@/sections/modulesSection/ModulesSection.tsx";

type LogsDialogProps = {
    trigger: ReactNode;
    runResult: RunResult;
};

export default function LogsDialog({ trigger, runResult }: LogsDialogProps) {
    const [open, setOpen] = useState(false);

    return (
        <Dialog modal open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{`${runResult.name} - Execution Log`}</DialogTitle>
                    <DialogDescription className="text-muted-foreground">
                        Detailed log output from the last process execution
                    </DialogDescription>
                </DialogHeader>
                <ScrollArea className="h-[500px] overflow-y-auto">
                    {runResult.logs.map((log, idx) => (
                        <p key={idx} className="text-sm whitespace-pre-wrap break-words">
                            {log}
                        </p>
                    ))}
                </ScrollArea>
            </DialogContent>
        </Dialog>
    );
}
