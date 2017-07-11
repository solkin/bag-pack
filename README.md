### Bag
Bag is a trivial archive format. 
It stores file inside as simple as possible, nothing more.

### Archive file format
`16-bit big endian` - file path length

`UTF-8` - file path

`64-bit big endian` - file size

`RAW` - file data
