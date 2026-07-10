import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { changePassword, uploadProfilePhoto } from "@/lib/api/auth";
import { getPersonnelPhotoUrl } from "@/lib/api/personnel";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import {
  User,
  Shield,
  Key,
  Camera,
  Loader2,
  Save,
  Phone,
  Briefcase,
  Building2,
  MapPin,
  Pencil,
} from "lucide-react";

export function ProfilePage() {
  const navigate = useNavigate();
  const { user, setUser } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  // Password form
  const [passwordForm, setPasswordForm] = useState({
    current_password: "",
    new_password: "",
    confirm_password: "",
  });

  if (!user) return null;

  const photoUrl = user.photo
    ? getPersonnelPhotoUrl(user.personnel_id)
    : null;

  async function handlePhotoChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      const updatedUser = await uploadProfilePhoto(file);
      setUser(updatedUser);
      addNotification("success", "Photo", "Photo de profil mise à jour");
    } catch {
      addNotification("error", "Erreur", "Impossible de mettre à jour la photo");
    } finally {
      setUploading(false);
    }
  }

  async function handlePasswordChange(e: React.FormEvent) {
    e.preventDefault();

    if (passwordForm.new_password !== passwordForm.confirm_password) {
      addNotification("error", "Erreur", "Les mots de passe ne correspondent pas");
      return;
    }

    if (passwordForm.new_password.length < 6) {
      addNotification("error", "Erreur", "Le mot de passe doit contenir au moins 6 caractères");
      return;
    }

    setSaving(true);
    try {
      await changePassword(passwordForm.current_password, passwordForm.new_password);
      addNotification("success", "Mot de passe", "Mot de passe modifié avec succès");
      setPasswordForm({
        current_password: "",
        new_password: "",
        confirm_password: "",
      });
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (
              err as {
                response: {
                  data: {
                    message: string;
                    errors?: Record<string, string>;
                  };
                };
              }
            ).response?.data?.message || "Erreur lors du changement de mot de passe"
          : "Erreur lors du changement de mot de passe";
      addNotification("error", "Erreur", msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Mon Profil</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gérez vos informations personnelles et votre mot de passe
          </p>
        </div>
        <Button
          onClick={() => navigate(`/personnel/${user.personnel_id}/edit`)}
          className="gap-2"
        >
          <Pencil className="h-4 w-4" />
          Modifier mes informations
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="md:col-span-1">
          <CardContent className="pt-6 flex flex-col items-center gap-3">
            <div className="relative">
              <div className="h-28 w-28 rounded-full overflow-hidden border-2 border-border bg-muted flex items-center justify-center">
                {photoUrl ? (
                  <img
                    src={photoUrl}
                    alt="Photo de profil"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <Camera className="h-10 w-10 text-muted-foreground" />
                )}
              </div>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                className="absolute bottom-0 right-0 h-8 w-8 rounded-full bg-primary text-primary-foreground flex items-center justify-center shadow-md hover:bg-primary/90 transition-colors"
              >
                {uploading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Camera className="h-4 w-4" />
                )}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                className="hidden"
                onChange={handlePhotoChange}
              />
            </div>
            <div className="text-center">
              <p className="font-semibold text-lg">
                {user.firstname} {user.lastname}
              </p>
              <p className="text-sm text-muted-foreground">{user.grade}</p>
            </div>
            <div className="text-center text-xs text-muted-foreground space-y-1">
              <p className="flex items-center gap-1 justify-center">
                <Shield className="h-3 w-3" />
                {user.role_name}
              </p>
              <p className="flex items-center gap-1 justify-center">
                <User className="h-3 w-3" />
                {user.username}
              </p>
            </div>
          </CardContent>
        </Card>

        <div className="md:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <User className="h-4 w-4" />
                Informations personnelles
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <Label className="text-xs text-muted-foreground">Nom</Label>
                  <p className="text-sm font-medium">{user.lastname}</p>
                </div>
                <div className="space-y-1">
                  <Label className="text-xs text-muted-foreground">Prénoms</Label>
                  <p className="text-sm font-medium">{user.firstname}</p>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <Label className="text-xs text-muted-foreground">IM</Label>
                  <p className="text-sm font-medium">{user.im}</p>
                </div>
                <div className="space-y-1">
                  <Label className="text-xs text-muted-foreground flex items-center gap-1">
                    <Phone className="h-3 w-3" /> Téléphone
                  </Label>
                  <p className="text-sm font-medium">{user.phone || "—"}</p>
                </div>
              </div>
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground flex items-center gap-1">
                  <MapPin className="h-3 w-3" /> Adresse
                </Label>
                <p className="text-sm font-medium">{user.address || "—"}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Briefcase className="h-4 w-4" />
                Informations professionnelles
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <Label className="text-xs text-muted-foreground">Grade</Label>
                  <p className="text-sm font-medium">{user.grade}</p>
                </div>
                <div className="space-y-1">
                  <Label className="text-xs text-muted-foreground flex items-center gap-1">
                    <Building2 className="h-3 w-3" /> Affectation
                  </Label>
                  <p className="text-sm font-medium">{user.affectation || "—"}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Key className="h-4 w-4" />
                Changer le mot de passe
              </CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handlePasswordChange} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="current_password">
                    Mot de passe actuel *
                  </Label>
                  <Input
                    id="current_password"
                    type="password"
                    value={passwordForm.current_password}
                    onChange={(e) =>
                      setPasswordForm({
                        ...passwordForm,
                        current_password: e.target.value,
                      })
                    }
                    required
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="new_password">
                      Nouveau mot de passe *
                    </Label>
                    <Input
                      id="new_password"
                      type="password"
                      value={passwordForm.new_password}
                      onChange={(e) =>
                        setPasswordForm({
                          ...passwordForm,
                          new_password: e.target.value,
                        })
                      }
                      required
                      minLength={6}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="confirm_password">
                      Confirmer le mot de passe *
                    </Label>
                    <Input
                      id="confirm_password"
                      type="password"
                      value={passwordForm.confirm_password}
                      onChange={(e) =>
                        setPasswordForm({
                          ...passwordForm,
                          confirm_password: e.target.value,
                        })
                      }
                      required
                      minLength={6}
                    />
                  </div>
                </div>
                <Button type="submit" className="gap-2" disabled={saving}>
                  {saving ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  {saving ? "Enregistrement..." : "Changer le mot de passe"}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </motion.div>
  );
}
