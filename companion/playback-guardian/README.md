# Playback guardian loading presentation

The optional Netflix wait activity shares the launcher's quiet loading style: an
opaque charcoal surface, official Netflix symbol, a small silver screen outline,
and one fixed status line beneath it. It leaves the bottom of the screen empty.
Waiting hides technical process messages. BACK/HOME can still cancel. A network
failure stops animation and exposes Retry and Cancel with the actual error.

The mark animates only while attached and visible, honors the system animation
setting, and releases its animator and settings observer on detach. It does not
fetch artwork or decode video. The launcher's per-app static/off preference is
not shared across packages; the guardian always honors system reduced animation.

Request tokens, focus checks, the network deadline, HOME/BACK cancellation and
exact Google-dialog matching are unchanged. The activity finishes immediately
after dispatching Netflix. This is a launch handoff, not proof of video playback.

The bundled Netflix symbol is the same official source used by Lightframe's dock;
see the main project's third-party notice. LoadingMarkView.java is kept identical
apart from its package declaration in both apps.
