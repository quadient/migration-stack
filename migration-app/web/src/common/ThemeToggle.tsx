import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import { useTheme } from "@/hooks/useTheme.ts";

export function ThemeToggle() {
    const [theme, setTheme] = useTheme();
    const next = theme === "dark" ? "light" : "dark";
    const label = `Switch to ${next} theme`;

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button variant="outline" size="icon" onClick={() => setTheme(next)}>
                    {theme === "dark" ? <Moon /> : <Sun />}
                    <span className="sr-only">{label}</span>
                </Button>
            </TooltipTrigger>
            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
}
