import { CircleCheckBig, CircleX, LoaderCircle } from "lucide-react";
import { cn } from "@/lib/utils.ts";
import type { RunStatus } from "@/types/job.ts";

export type RunSummaryItem = { name: string; status?: RunStatus };

export function RunLabel({ items }: { items: RunSummaryItem[] }) {
    return (
        <>
            <span className="truncate">{items[0]?.name}</span>
            {items.length > 1 && <span className="shrink-0 text-xs opacity-70">{`+${items.length - 1}`}</span>}
        </>
    );
}

export function RunModuleList({ items }: { items: RunSummaryItem[] }) {
    return (
        <ol className="flex flex-col gap-1">
            {items.map((item, index) => (
                <li key={index} className="flex items-center gap-1.5">
                    {item.status && <RunStatusIcon status={item.status} className="size-3.5 text-current" />}
                    {`${index + 1}. ${item.name}`}
                </li>
            ))}
        </ol>
    );
}

export function RunStatusIcon({ status, className }: { status: RunStatus; className?: string }) {
    switch (status) {
        case "RUNNING":
            return <LoaderCircle className={cn("animate-spin text-status-running", className)} />;
        case "SUCCESS":
            return <CircleCheckBig className={cn("text-status-ok", className)} />;
        case "ERROR":
            return <CircleX className={cn("text-status-fail", className)} />;
    }
}
