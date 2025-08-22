import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";
import { type IpsConfig, type MigrationConfig, type Settings } from "@/dialogs/settings/settingsTypes.tsx";

type MigrationSettingsFormProps = {
    settings: Settings;
    setSettings: (value: ((prevState: Settings | null) => Settings | null) | Settings | null) => void;
};

export function MigrationSettingsForm({ settings, setSettings }: MigrationSettingsFormProps) {
    const migrationConfig = settings.migrationConfig;

    const updateMigrationConfig = (key: keyof MigrationConfig, value: any) => {
        setSettings((prev) =>
            prev
                ? {
                      ...prev,
                      migrationConfig: {
                          ...prev.migrationConfig,
                          [key]: value,
                      },
                  }
                : null,
        );
    };

    const updateDbConfig = (key: string, value: any) => {
        setSettings((prev) =>
            prev
                ? {
                      ...prev,
                      migrationConfig: {
                          ...prev.migrationConfig,
                          dbConfig: {
                              ...prev.migrationConfig.dbConfig,
                              [key]: value,
                          },
                      },
                  }
                : null,
        );
    };

    const updateIpsConfig = (key: keyof IpsConfig, value: any) => {
        setSettings((prev) =>
            prev
                ? {
                      ...prev,
                      migrationConfig: {
                          ...prev.migrationConfig,
                          inspireConfig: {
                              ...prev.migrationConfig.inspireConfig,
                              ipsConfig: {
                                  ...prev.migrationConfig.inspireConfig.ipsConfig,
                                  [key]: value,
                              },
                          },
                      },
                  }
                : null,
        );
    };

    return (
        <div className="grid gap-5">
            <div className="font-bold">Connections</div>
            <div className="grid gap-4">
                <div className="grid gap-3">
                    <Label>Storage root</Label>
                    <Input
                        value={migrationConfig.storageRoot ?? ""}
                        onChange={(e) => updateMigrationConfig("storageRoot", e.target.value)}
                    />
                </div>
                <div className="font-normal">Database</div>
                <div className="grid gap-3">
                    <Label>Host</Label>
                    <Input
                        value={migrationConfig.dbConfig.host}
                        onChange={(e) => updateDbConfig("host", e.target.value)}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Port</Label>
                    <Input
                        type="number"
                        value={migrationConfig.dbConfig.port}
                        onChange={(e) => updateDbConfig("port", Number(e.target.value))}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Name</Label>
                    <Input
                        value={migrationConfig.dbConfig.dbName}
                        onChange={(e) => updateDbConfig("dbName", e.target.value)}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>User</Label>
                    <Input
                        value={migrationConfig.dbConfig.user}
                        onChange={(e) => updateDbConfig("user", e.target.value)}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Password</Label>
                    <Input
                        type="password"
                        value={migrationConfig.dbConfig.password}
                        onChange={(e) => updateDbConfig("password", e.target.value)}
                    />
                </div>
                <div className="font-normal">Inspire Production Server</div>
                <div className="grid gap-3">
                    <Label>Host</Label>
                    <Input
                        value={migrationConfig.inspireConfig.ipsConfig.host}
                        onChange={(e) => updateIpsConfig("host", e.target.value)}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Port</Label>
                    <Input
                        type="number"
                        value={migrationConfig.inspireConfig.ipsConfig.port}
                        onChange={(e) => updateIpsConfig("port", Number(e.target.value))}
                    />
                </div>
                <div className="grid gap-3">
                    <Label>Timeout (seconds)</Label>
                    <Input
                        type="number"
                        value={migrationConfig.inspireConfig.ipsConfig.timeoutSeconds}
                        onChange={(e) => updateIpsConfig("timeoutSeconds", Number(e.target.value))}
                    />
                </div>
            </div>
        </div>
    );
}
