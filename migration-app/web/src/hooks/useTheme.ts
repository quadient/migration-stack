import { useEffect, useState } from "react";

const THEME_STORAGE_KEY = "theme";
const DARK_SCHEME_QUERY = "(prefers-color-scheme: dark)";

export type Theme = "light" | "dark";

export function useTheme(): [Theme, (theme: Theme) => void] {
    const [storedTheme, setStoredTheme] = useState(loadTheme);
    const [systemTheme, setSystemTheme] = useState(getSystemTheme);
    const theme = storedTheme ?? systemTheme;

    useEffect(() => {
        const darkScheme = window.matchMedia(DARK_SCHEME_QUERY);
        const onChange = () => setSystemTheme(getSystemTheme());
        darkScheme.addEventListener("change", onChange);
        return () => darkScheme.removeEventListener("change", onChange);
    }, []);

    useEffect(() => applyTheme(theme), [theme]);

    const changeTheme = (next: Theme) => {
        setStoredTheme(next);
        saveTheme(next);
    };

    return [theme, changeTheme];
}

export function applyTheme(theme: Theme = loadTheme() ?? getSystemTheme()) {
    document.documentElement.classList.toggle("dark", theme === "dark");
    document.documentElement.style.colorScheme = theme;
}

function getSystemTheme(): Theme {
    return window.matchMedia(DARK_SCHEME_QUERY).matches ? "dark" : "light";
}

function loadTheme(): Theme | undefined {
    try {
        const theme = localStorage.getItem(THEME_STORAGE_KEY);
        return theme === "light" || theme === "dark" ? theme : undefined;
    } catch {
        return undefined;
    }
}

function saveTheme(theme: Theme) {
    try {
        localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
        // ignore, not a big deal
    }
}
