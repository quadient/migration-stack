import { useSidebar } from "@/components/ui/sidebar.tsx";

type SidebarResizeHandleProps = {
    onResize: (width: number) => void;
    onResizingChange: (resizing: boolean) => void;
};

export function SidebarResizeHandle({ onResize, onResizingChange }: SidebarResizeHandleProps) {
    const { isMobile, state } = useSidebar();

    if (isMobile || state === "collapsed") {
        return null;
    }

    return (
        <div
            role="separator"
            aria-orientation="vertical"
            onPointerDown={(event) => {
                event.preventDefault();
                event.currentTarget.setPointerCapture(event.pointerId);
                onResizingChange(true);
            }}
            onPointerMove={(event) => {
                if (event.currentTarget.hasPointerCapture(event.pointerId)) {
                    onResize(event.clientX);
                }
            }}
            onLostPointerCapture={() => onResizingChange(false)}
            className="fixed inset-y-0 left-[calc(var(--sidebar-width)-4px)] z-20 w-2 cursor-col-resize after:absolute after:inset-y-0 after:left-1/2 after:w-0.5 hover:after:bg-sidebar-border"
        />
    );
}
