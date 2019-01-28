meta:
  id: demo
  file-extension: demo
  endian: le
seq:
  - id: header
    type: header
types:
  header:
    seq:
      - id: Magic
        contents: 'DEMO'
