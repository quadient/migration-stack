import { LayoutDashboard, ListTree } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import type { View } from "@/hooks/useView.ts";

type ViewToggleProps = {
    view: View;
    onViewChange: (view: View) => void;
};

export function ViewToggle({ view, onViewChange }: ViewToggleProps) {
    const next = view === "dashboard" ? "runner" : "dashboard";
    const label = next === "runner" ? "Switch to task runner" : "Switch to dashboard";

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button variant="outline" size="icon" onClick={() => onViewChange(next)}>
                    {next === "runner" ? <ListTree /> : <LayoutDashboard />}
                    <span className="sr-only">{label}</span>
                </Button>
            </TooltipTrigger>
            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
}
