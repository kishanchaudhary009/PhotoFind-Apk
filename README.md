# PhotoFind AI

Search your phone gallery using natural language, completely offline using on-device AI.

## Overview

Finding a specific image in a gallery containing thousands of photos is often frustrating.

Whether you're searching for:

- Aadhaar Card
- PAN Card
- A photo wearing a blue kurta
- A picture with your cat
- A specific object or scene

PhotoFind AI lets you search your gallery using simple natural language instead of scrolling through thousands of images.

Everything runs locally on your device, so your photos never leave your phone.

## Features

- Natural language image search
- Fully offline inference
- On-device AI
- Privacy-first design
- Optimized for low-end Android devices
- Fast semantic image retrieval

## Example Searches

```
aadhar card
pan card
man standing in blue kurta
my photo with a cat
person wearing sunglasses
black backpack
red car
```

## How It Works

Every image in the gallery is converted into an embedding using an image encoder.

When a user enters a query, a text encoder converts the text into another embedding.

The application compares both embeddings using cosine similarity and returns the closest matching images.

```
Gallery Images
      │
      ▼
Image Encoder
      │
      ▼
Image Embeddings

User Query
      │
      ▼
Text Encoder
      │
      ▼
Text Embedding
      │
      ▼
Cosine Similarity
      │
      ▼
Matching Images
```

## Model

The application currently uses **ViT-B-32** pretrained on **laion2b_s34b_b79k**.

The image and text encoders generate embeddings that enable semantic image search directly on the device.

## Optimization

Running vision-language models on mobile devices requires careful optimization.

The original models occupied nearly **600 MB**.

Both encoders were quantized to **INT8**, reducing their combined size to approximately **150 MB**.

- Image Encoder: 85 MB
- Text Encoder: 65 MB

This allows both models to be bundled directly inside the application while keeping the overall app size below **170 MB**.

## Performance

The application is designed to work even on devices with **3 GB RAM**.

For devices with more memory, inference is accelerated using parallel Kotlin coroutines together with ONNX Runtime.

## Current Limitation

The current models are general-purpose pretrained encoders.

They do not yet understand certain domain-specific terms such as:

- Aadhaar Card
- PAN Card
- Driving License
- Identity Card

Fine-tuning is currently in progress to improve retrieval accuracy for real-world gallery search.

## Tech Stack

- Kotlin
- Android
- ONNX Runtime
- Kotlin Coroutines
- OpenCLIP
- ViT-B-32
- INT8 Quantization

## Roadmap

- Fine-tuned encoders
- Improved semantic search
- Hugging Face model release
- Windows desktop version
- Float16 desktop models for higher accuracy
- Faster indexing
- Multi-language support

## Privacy

PhotoFind AI performs all inference locally.

No images are uploaded, no internet connection is required, and no user data leaves the device.

## License

MIT License
