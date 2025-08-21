import React from "react";

type UseFetchResult<T> =
    | {
          status: "error";
          error: unknown;
      }
    | {
          status: "loading";
      }
    | {
          data: T;
          status: "ok";
      };

export function useFetch<T>(url: string, defaultValue: T): UseFetchResult<T> {
    const [status, setStatus] = React.useState("loading");
    const [error, setError] = React.useState<unknown | null>(undefined);
    const [data, setData] = React.useState<T>(defaultValue);

    React.useEffect(() => {
        let cancelled = false;
        const controller = new AbortController();
        fetch(url, { method: "GET", signal: controller.signal })
            .then((result) => {
                result
                    .json()
                    .then((json) => {
                        setData(json);
                        setStatus("ok");
                    })
                    .catch((err) => {
                        setStatus("error");
                        setError(err);
                    });
            })
            .catch((err) => {
                setStatus("error");
                setError(err);
            });

        return () => {
            controller.abort();
            cancelled = true;
        };
    }, [url]);

    return { data, error, status } as UseFetchResult<T>;
}
