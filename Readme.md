这是读IMU、GPS、罗盘、相机的程序。安卓6.0以上应该就能用。
这个安装了以后，可能第一次打开会崩溃。不过它会请求几个权限，每个权限都同意就好了。
然后打开的时候，会出现一个对话框，你选前置或者后置相机就行。选无的话，应该会崩溃。
之后，右边有两个选项，下面那个摄像的，打钩，就不摄像了。不打也行，就会一直录像比较耗空间耗电。
上面的选择框，可以选录像或者记录图像序列。录像格式为MP4，图像格式为YUV_420_888。
结束的时候，点结束就好。
然后下次再想录，就得重启程序！！
录完了的数据，在根文件夹里，a_MotionRecord里。每次记录的数据都存在一个文件夹里，文件夹以时间命名。有四个TXT文件，如果你选择录视频或者照片，就会出现一个视频或者好多照片。
Acc、Gyro、Mag是IMU和磁力计数据。里面的数据，每行是一组数据。第一个数是IMU的时间戳，之后三个是xyz的分量（mag可能要标定一下，手机画八字就好。），最后一个数据是整个手机的时间戳，为了和GPS、相机同步的。
CAM文件里，第一个是CAM的时间戳，第二是曝光时间，第三是Rolling Shutter的时间，第四个是整个手机的时间戳。
GPS里，第一个是GPS时间戳，第二个是lon，第三个lat，第四个alt，第五个速度，第六个速度方向，第七个误差大小（米），第八个系统时间戳。

