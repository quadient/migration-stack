import { type ReactNode, useEffect, useLayoutEffect, useRef } from "react";
import { ArrowDownToLine, ArrowUpToLine } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Card, CardContent } from "@/components/ui/card.tsx";
import { useRequest } from "@/hooks/useRequest.ts";
import type { Job } from "@/types/job.ts";

const STICK_TO_BOTTOM_THRESHOLD_PX = 16;
const RECENT_GROWTH_WINDOW_MS = 50;
const SCROLL_UP_KEYS = ["ArrowUp", "PageUp", "Home"];

type JobLogsProps = {
    job: Job;
    setJobs: (value: ((prevState: Job[]) => Job[]) | Job[]) => void;
};

type Growth = { time: number; px: number };

export function JobLogs({ job, setJobs }: JobLogsProps) {
    useRequest<Job>({
        url: `api/job?id=${job.id}`,
        onSuccess: (job) => setJobs((prev) => prev.map((it) => (it.id === job.id ? job : it))),
        onError: (error) => console.error("Failed to fetch job logs:", error),
        condition: job.logs === undefined,
    });

    const viewportRef = useRef<HTMLDivElement>(null);
    const stickToBottom = useRef(true);
    const lastScrollTop = useRef(0);
    const lastScrollHeight = useRef(0);
    const recentGrowth = useRef<Growth[]>([]);

    useEffect(() => {
        const viewport = viewportRef.current;
        if (!viewport) {
            return;
        }

        // Scroll events arrive a frame late, after new logs could already have scrolled back down,
        // so user input has to stop the following right away
        const stopFollowing = () => {
            stickToBottom.current = false;
        };
        const onWheel = (event: WheelEvent) => event.deltaY < 0 && stopFollowing();
        const onKeyDown = (event: KeyboardEvent) => SCROLL_UP_KEYS.includes(event.key) && stopFollowing();
        const onPointerDown = (event: PointerEvent) =>
            event.target === viewport && event.offsetX >= viewport.clientWidth && stopFollowing();
        const onScroll = () => {
            const distance = distanceFromBottom(viewport);
            if (viewport.scrollTop < lastScrollTop.current && distance > STICK_TO_BOTTOM_THRESHOLD_PX) {
                stopFollowing();
            } else if (distance <= STICK_TO_BOTTOM_THRESHOLD_PX + sumGrowth(recentGrowth.current)) {
                // Logs that arrived since the scroll moved the bottom away, so the user reaching
                // the bottom as it was a moment ago still counts
                stickToBottom.current = true;
            }
            lastScrollTop.current = viewport.scrollTop;
        };

        viewport.addEventListener("wheel", onWheel, { passive: true });
        viewport.addEventListener("keydown", onKeyDown);
        viewport.addEventListener("pointerdown", onPointerDown);
        viewport.addEventListener("scroll", onScroll);
        return () => {
            viewport.removeEventListener("wheel", onWheel);
            viewport.removeEventListener("keydown", onKeyDown);
            viewport.removeEventListener("pointerdown", onPointerDown);
            viewport.removeEventListener("scroll", onScroll);
        };
    }, []);

    useLayoutEffect(() => {
        stickToBottom.current = true;
    }, [job.id]);

    useLayoutEffect(() => {
        const viewport = viewportRef.current;
        if (!viewport) {
            return;
        }

        const growth = viewport.scrollHeight - lastScrollHeight.current;
        recentGrowth.current = [
            ...pruneGrowth(recentGrowth.current),
            { time: performance.now(), px: Math.max(growth, 0) },
        ];
        lastScrollHeight.current = viewport.scrollHeight;

        if (stickToBottom.current) {
            viewport.scrollTop = viewport.scrollHeight;
            lastScrollTop.current = viewport.scrollTop;
        }
    }, [job.id, job.logs]);

    const scrollToTop = () => {
        const viewport = viewportRef.current;
        if (viewport) {
            stickToBottom.current = false;
            viewport.scrollTop = 0;
        }
    };

    const scrollToBottom = () => {
        const viewport = viewportRef.current;
        if (viewport) {
            stickToBottom.current = true;
            viewport.scrollTop = viewport.scrollHeight;
            lastScrollTop.current = viewport.scrollTop;
        }
    };

    return (
        <Card className="flex flex-col h-full w-full overflow-hidden py-2">
            <CardContent className="relative flex flex-1 overflow-hidden px-2">
                <div
                    ref={viewportRef}
                    tabIndex={0}
                    style={{ overflowWrap: "break-word", wordBreak: "break-word" }}
                    className="flex-1 min-w-0 overflow-y-auto pr-2 rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 [scrollbar-color:var(--border)_transparent] [scrollbar-width:thin]"
                >
                    {job.logs?.map((log, idx) => (
                        <div key={idx} className="text-sm mb-1 pl-[1em] indent-[-1em]">
                            {log}
                        </div>
                    ))}
                </div>
                <div className="absolute right-5 bottom-0 flex flex-col gap-1">
                    <ScrollButton label="Scroll to top" onClick={scrollToTop}>
                        <ArrowUpToLine />
                    </ScrollButton>
                    <ScrollButton label="Scroll to bottom" onClick={scrollToBottom}>
                        <ArrowDownToLine />
                    </ScrollButton>
                </div>
            </CardContent>
        </Card>
    );
}

type ScrollButtonProps = {
    label: string;
    onClick: () => void;
    children: ReactNode;
};

function ScrollButton({ label, onClick, children }: ScrollButtonProps) {
    return (
        <Button
            variant="outline"
            size="icon"
            className="size-7 opacity-70 hover:opacity-100"
            aria-label={label}
            title={label}
            onClick={onClick}
        >
            {children}
        </Button>
    );
}

function distanceFromBottom(viewport: HTMLElement): number {
    return viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop;
}

function pruneGrowth(growth: Growth[]): Growth[] {
    const now = performance.now();
    return growth.filter((it) => now - it.time <= RECENT_GROWTH_WINDOW_MS);
}

function sumGrowth(growth: Growth[]): number {
    return pruneGrowth(growth).reduce((sum, it) => sum + it.px, 0);
}
