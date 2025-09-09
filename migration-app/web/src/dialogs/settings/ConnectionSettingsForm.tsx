import { Label } from "@/components/ui/label.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
    type DbConfig,
    type IpsConfig,
    type MigrationConfig,
    type SettingsFormProps,
} from "@/dialogs/settings/settingsTypes.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function ConnectionSettingsForm({ settings, setSettings }: SettingsFormProps) {
    const migrationConfig = settings.migrationConfig;

    const updateMigrationConfig = (key: keyof MigrationConfig, value: any) => {
        setSettings((prev) => ({
            ...prev,
            migrationConfig: {
                ...prev.migrationConfig,
                [key]: value,
            },
        }));
    };

    const updateDbConfig = (key: keyof DbConfig, value: any) => {
        setSettings((prev) => ({
            ...prev,
            migrationConfig: {
                ...prev.migrationConfig,
                dbConfig: {
                    ...prev.migrationConfig.dbConfig,
                    [key]: value,
                },
            },
        }));
    };

    const updateIpsConfig = (key: keyof IpsConfig, value: any) => {
        setSettings((prev) => ({
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
        }));
    };

    return (
        <div className="grid gap-6">
            <Card>
                <CardHeader>
                    <CardTitle>Storage</CardTitle>
                    <CardDescription>Configuration of Storage for binary files </CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
                    <div className="grid gap-3">
                        <Label>Root Path</Label>
                        <Input
                            value={migrationConfig.storageRoot ?? ""}
                            onChange={(e) => updateMigrationConfig("storageRoot", e.target.value)}
                        />
                    </div>
                </CardContent>
            </Card>
            <Card>
                <CardHeader>
                    <CardTitle>Database</CardTitle>
                    <CardDescription>Database connection configuration</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
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
                </CardContent>
            </Card>
            <Card>
                <CardHeader>
                    <CardTitle>Inspire Production Server</CardTitle>
                    <CardDescription>Inspire Production Server configuration</CardDescription>
                </CardHeader>
                <CardContent className="grid gap-6">
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
                </CardContent>
            </Card>
        </div>
    );
}
