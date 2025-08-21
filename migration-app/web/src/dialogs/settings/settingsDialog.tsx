import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogTrigger,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog.tsx";
import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";
import type { ReactNode } from "react";
import * as React from "react";
import type { Settings } from "@/dialogs/settings/settingsTypes.tsx";
import { Button } from "@/components/ui/button.tsx";

type SettingsDialogProps = {
  trigger: ReactNode;
};

export default function SettingsDialog({ trigger }: SettingsDialogProps) {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  console.log(settings);

  useEffect(() => {
    fetchSettings()
      .then((json) => setSettings(json))
      .catch(() => {
        console.error("Failed to fetch or parse settings");
        setSettings(null);
      });
  }, []);

  return (
    <Dialog modal open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Settings</DialogTitle>
          <DialogDescription className="text-gray-500">
            Configure project settings and connection to services
          </DialogDescription>
        </DialogHeader>
        {settings && (
          <div className="max-h-100 overflow-y-auto">
            {renderSettings(settings, setSettings)}
          </div>
        )}
        <DialogFooter>
          <Button
            type={"submit"}
            onClick={() => {
              setIsOpen(false);
              return onSaveChanges(settings);
            }}
          >
            Save Changes
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function renderSettings(
  settings: Settings,
  setSettings: (
    value: ((prevState: Settings | null) => Settings | null) | Settings | null,
  ) => void,
): React.ReactNode {
  return (
    <div className="grid gap-5">
      <div className="text-lg font-bold">Project Settings</div>
      <div className="grid gap-4">
        <div className="grid gap-3">
          <Label className="grid gap-4">Name</Label>
          <Input
            value={settings.projectConfig.name}
            onChange={(e) =>
              setSettings((prevSettings) =>
                prevSettings
                  ? {
                      ...prevSettings,
                      projectConfig: {
                        ...prevSettings.projectConfig,
                        name: e.target.value,
                      },
                    }
                  : null,
              )
            }
          />
        </div>
      </div>
    </div>
  );
}

async function onSaveChanges(settings: Settings | null): Promise<void> {
  if (!settings) return;

  const response = await fetch("/api/settings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(settings),
  });
  if (!response.ok) {
    throw new Error("Failed to save settings");
  }
}

async function fetchSettings(): Promise<Settings> {
  const response = await fetch("/api/settings");
  if (!response.ok) {
    throw new Error("Failed to fetch settings");
  }
  return await response.json();
}
