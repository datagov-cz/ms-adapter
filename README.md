# Adaptér při přístup ke službám skrze Microsoft Graph API

Adaptér vyžaduje konfiguraci aplikace skrze [Microsoft Entra](https://entra.microsoft.com/).
Tam je třeba přejít na "Zobrazit aplikace".

## Konfigurace

Vybrané argumenty aplikace je možné nastavit skrze proměnné prostředí.
- `MS_APPLICATION` - Identifikátor aplikace.
  Lze získat v "Přehled" registrované aplikace jako "ID aplikace (klienta)".
- `MS_TENANT` - Identifikátor tenanta.
  Lze získat v "Přehled" detailu registrované aplikace jako "ID adresáře (tenanta)".
- `MS_SECRET` - Hodnota tajemství aplikace.
  Lze získat v "Certifikáty a tajné kódy" registrované aplikace jako "Hodnota" v záložce "Tajné kódy klienta".

## Stažení SharePoint seznamu

Argumenty:
- `application` - Nahrazuje hodnotu z `MS_APPLICATION`. Volitelný argument.
- `tenant` - Nahrazuje hodnotu z `MS_TENANT`. Volitelný argument.
- `secret` - Nahrazuje hodnotu z `MS_SECRET`. Volitelný argument.
- `site` - Identifikátor stránky, získání je popsáno v samostatné sekci.
- `list` - Identifikátor seznamu, získání je popsáno v samostatné sekci.
- `base` - Predikáty v seznamu jsou tvořeny jako `{base}#{jméno-sloupce}`.
- `output` - Cesta k souboru pro uložení obsahu seznamu ve formátu TriG.

Příklad spuštění:
```bash
java -jar microsoft-adapter.jar download-list --application {application} --tenant {tenant} --secret {secret} --site {site} --list {list} --base {base URL} --output {output}
```

## Stažení obsahu adresáře ze SharePointu

Argumenty:
- `application` - Nahrazuje hodnotu z `MS_APPLICATION`. Volitelný argument.
- `tenant` - Nahrazuje hodnotu z `MS_TENANT`. Volitelný argument.
- `secret` - Nahrazuje hodnotu z `MS_SECRET`. Volitelný argument.
- `site` - Identifikátor stránky, získání je popsáno v samostatné sekci.
- `path` - Název "Knihovny dokumentů" v "Obsahu webu" následovaný jmény adresářů. Znak `/` slouží jako oddělovač.
- `output` - Cesta k adresáři kam uložit soubory.

Příklad spuštění:
```bash
java -jar microsoft-adapter.jar download-directory --application {application} --tenant {tenant} --secret {secret} --site {site} --drive {drive} --directory {directory} --output {output}
```

## Získání identifikátoru stránky a seznamu

K získání některých hodnot je možné použít [Graph Explorer](https://developer.microsoft.com/en-us/graph/graph-explorer).

K získání identifikátoru stránky je možné použít následující dotaz:
```
https://graph.microsoft.com/v1.0/sites?$select=id&search={název stránky}
```
Pokud je odpovědí `403` je třeba udělit oprávnění `Sites.Read.All` v záložce `Modify permissions`.

K získání identifikátoru seznamu je možné otevřít nejprve seznam v prohlížeči.
Dále pak v sekci nastavení, tlačítko kolečka vpravo nahoře, je možné se navigovat na "Nastavení seznamu".
Identifikátor je pak součástí URL:
```
https://...sharepoint.com/sites/.../_layouts/.../listedit.aspx?List=%7B{identifikátor seznamu}%7D
```
