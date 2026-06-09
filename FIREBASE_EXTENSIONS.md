# Firebase Extensions

## Resize Images (storage-resize-images)

Automatycznie generuje miniaturki zdjęć po uploadzie do Storage.

### Instalacja:
```bash
firebase ext:install storage-resize-images --project=playground-705e7162
```

### Konfiguracja:
- Source path: `places/`
- Resized images path: `places_thumbs/`
- Sizes: `200x200, 400x400`
- Delete original: No

### Koszt:
- FREE (Blaze plan required for extensions, but the extension itself is free)
- Cloud Functions invocations: ~1 per upload (within free tier)
