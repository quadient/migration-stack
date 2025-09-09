import React, { type DependencyList } from "react";

export type UseRequestProps<T> = {
    url: string;
    onSuccess: (data: T) => void;
    onError: (error: unknown) => void;
    requestInit?: RequestInit;
    deps?: DependencyList;
    condition?: boolean;
};

export function useRequest<T>({ url, onSuccess, onError, requestInit, deps, condition }: UseRequestProps<T>): void {
    React.useEffect(() => {
        if (condition === false) {
            return;
        }

        let cancelled = false;
        const controller = new AbortController();
        const init: RequestInit = requestInit ?? {};
        init.signal = controller.signal;
        if (!init.method) {
            init.method = "GET";
        }

        fetch(url, init)
            .then((result) => {
                if (cancelled) return;
                result
                    .json()
                    .then((json) => {
                        if (cancelled) return;
                        onSuccess(json as T);
                    })
                    .catch((err) => {
                        if (cancelled) return;
                        onError(err);
                    });
            })
            .catch((err) => {
                if (cancelled) return;
                onError(err);
            });

        return () => {
            controller.abort();
            cancelled = true;
        };
    }, [url, ...(deps ?? [])]);
}
