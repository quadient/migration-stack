import type { RunStatus } from "@/types/job.ts";
import { Badge } from "@/components/ui/badge.tsx";
import { CircleCheckBig, CircleX, LoaderCircle } from "lucide-react";

export function StatusBadge({ runStatus }: { runStatus: RunStatus }) {
    if (runStatus === "RUNNING") {
        return (
            <Badge className="bg-blue-100 text-blue-800">
                <>
                    <LoaderCircle className="animate-spin" />
                    Running
                </>
            </Badge>
        );
    } else if (runStatus === "SUCCESS") {
        return (
            <Badge className="bg-green-100 text-green-800">
                <>
                    <CircleCheckBig />
                    Success
                </>
            </Badge>
        );
    } else if (runStatus === "ERROR") {
        return (
            <Badge className="bg-red-100 text-red-800">
                <>
                    <CircleX />
                    Error
                </>
            </Badge>
        );
    } else {
        return null;
    }
}
