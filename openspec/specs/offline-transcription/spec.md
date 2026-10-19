## Purpose

Provide on-device speech-to-text via bundled whisper.cpp, including model provisioning on first run and behavior when transcription fails.

## Requirements

### Requirement: Transcription is fully offline

Speech-to-text SHALL be performed entirely on device using a bundled whisper.cpp model. The app SHALL NOT declare the `INTERNET` permission.

#### Scenario: Capture with no network

- **WHEN** the device is in airplane mode with no network of any kind
- **THEN** a capture SHALL transcribe and produce a note normally

#### Scenario: No network permission

- **WHEN** the app manifest is inspected
- **THEN** it SHALL NOT contain the `android.permission.INTERNET` permission

### Requirement: Model is bundled and provisioned on first run

A quantized `base.en` GGML model SHALL ship in the APK assets and SHALL be extracted to internal storage on first use, because whisper requires a filesystem path.

#### Scenario: First capture after install

- **WHEN** a capture is transcribed for the first time after installation
- **THEN** the model SHALL be extracted from assets to internal storage
- **AND** transcription SHALL proceed using the extracted file

#### Scenario: Subsequent captures reuse the extracted model

- **WHEN** a capture is transcribed and the model has already been extracted
- **THEN** the extraction SHALL be skipped

#### Scenario: Extraction is interrupted

- **WHEN** model extraction fails or is interrupted partway
- **THEN** the partial file SHALL NOT be treated as valid
- **AND** extraction SHALL be reattempted on the next capture

### Requirement: Model loading overlaps recording

Model loading SHALL begin concurrently with recording rather than blocking it, so that load time is absorbed by the time the user spends speaking.

#### Scenario: Load during speech

- **WHEN** a capture session starts
- **THEN** model loading SHALL be initiated without delaying the start of audio capture or the start haptic

### Requirement: Transcription failure does not lose the capture

If transcription fails for any reason, the recorded audio SHALL remain on disk and the capture SHALL be reported as needing attention. The report SHALL identify which class of failure occurred rather than reporting an undifferentiated failure.

#### Scenario: Model fails to load

- **WHEN** the model cannot be loaded
- **THEN** the recorded audio file SHALL remain in the pending location
- **AND** the failure signal SHALL be raised
- **AND** the reported outcome SHALL identify the model as unavailable

#### Scenario: Transcription returns empty text

- **WHEN** transcription completes but produces no text, or only whitespace
- **THEN** no empty note SHALL be written
- **AND** the recorded audio SHALL remain in the pending location
- **AND** the failure signal SHALL be raised
- **AND** the reported outcome SHALL identify that no speech was detected

#### Scenario: Transcription returns only a non-speech placeholder

- **WHEN** transcription completes and returns only a bracketed non-speech placeholder
- **THEN** no note SHALL be written
- **AND** the reported outcome SHALL identify that no speech was detected, distinct from an error

#### Scenario: The recognizer or the note write throws

- **WHEN** the recognizer throws, or the transcript cannot be written to the vault
- **THEN** the recorded audio SHALL remain in the pending location
- **AND** the reported outcome SHALL identify which of the two stages failed, and SHALL carry the underlying cause

#### Scenario: Process is killed mid-transcription

- **WHEN** the app process is terminated while transcription is in progress
- **THEN** the recorded audio SHALL remain in the pending location for later retry

### Requirement: Audio that is gone before its turn is not a failure

The transcription queue serializes work behind a single loaded model, so an entry can wait an arbitrary time before it runs. A queued entry whose audio file no longer exists when its turn arrives SHALL be treated as already handled and SHALL NOT be reported as a failure. Existence SHALL be verified after the queue lock is acquired, not only when the entry was enqueued.

#### Scenario: The file was transcribed by another queue entry while waiting

- **WHEN** a queued entry acquires the transcription lock and its audio file has been deleted by the entry that transcribed it
- **THEN** the outcome SHALL be reported as already handled
- **AND** no failure SHALL be raised for that capture

#### Scenario: The user discarded the audio while it was queued

- **WHEN** the user discards pending audio and a queued entry for that same audio subsequently acquires the lock
- **THEN** no failure SHALL be raised for that capture

#### Scenario: The recognizer is never invoked on a missing path

- **WHEN** a queued entry's audio file does not exist at the moment it would be transcribed
- **THEN** the recognizer SHALL NOT be invoked with that path

### Requirement: The same capture is not transcribed concurrently

The queue SHALL NOT hold more than one outstanding entry for the same audio file, so that a retry sweep cannot enqueue work that is already in flight.

#### Scenario: A sweep overlaps an in-flight transcription

- **WHEN** a retry sweep enumerates pending audio while one of those files is already being transcribed
- **THEN** that file SHALL NOT be enqueued a second time

### Requirement: Native resources are released

Loaded model resources SHALL be released once the transcription queue is empty, so that a long-lived service does not retain native memory indefinitely.

#### Scenario: Queue drains

- **WHEN** the last pending transcription completes and no further captures are queued
- **THEN** the loaded model SHALL be released
