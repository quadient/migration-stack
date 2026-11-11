import { useEffect, useState } from "react";

export type View = "dashboard" | "runner";

const VIEW_URL_PARAM = "view";

export function useView(): [View, (view: View) => void] {
    const [view, setView] = useState(loadView);

    useEffect(() => {
        const onPopState = () => setView(loadView());
        window.addEventListener("popstate", onPopState);
        return () => window.removeEventListener("popstate", onPopState);
    }, []);

    const changeView = (view: View) => {
        window.history.pushState(null, "", getViewUrl(view));
        setView(view);
    };

    return [view, changeView];
}

function loadView(): View {
    const view = new URLSearchParams(window.location.search).get(VIEW_URL_PARAM);
    return view === "runner" ? "runner" : "dashboard";
}

function getViewUrl(view: View): URL {
    const url = new URL(window.location.pathname, window.location.origin);
    if (view === "runner") {
        url.searchParams.set(VIEW_URL_PARAM, view);
    }
    return url;
}
