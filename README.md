# NetListening
A super fast Java event and listener based networking library,
the goal of this project is to help its users to develop java applications using TCP.
NetListening offers encryption, compression and many other mechanics.
The only dependency which has to be available at runtime is Netty.

If you want to learn how to use the NetListening library, just look at the example project: https://github.com/terrarier2111/NetListeningChat

If you have any questions or just want to get in touch with the community, here is the link to our official discord: https://discord.gg/BGtZ5fpQKS

System properties:
de.terrarier.netlistening.IgnoreEmptyPackets - bool: Whether empty packets should be dropped or not.           (def: true)
de.terrarier.netlistening.CheckBounds        - bool: Whether buffers should be checked for OOB, when accessed. (def: false)

NOTE: The following System properties are experimental exclusively, default values may be changed or the property may be removed entirely in the future.
de.terrarier.netlistening.MaxFrameSize       -  int: The max size a frame may have until its dropped in bytes. (def: 1024 * 1024 * 16 | aka 16 MB)
