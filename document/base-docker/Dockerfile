FROM ubuntu:24.04

RUN sed -i 's@//.*archive.ubuntu.com@//mirrors.aliyun.com@g' /etc/apt/sources.list.d/ubuntu.sources &&\
    sed -i 's@//security.ubuntu.com@//mirrors.aliyun.com@g' /etc/apt/sources.list.d/ubuntu.sources &&\
    sed -i 's@//ports.ubuntu.com@//mirrors.aliyun.com@g' /etc/apt/sources.list.d/ubuntu.sources &&\
    apt-get update &&\
    export DEBIAN_FRONTEND=noninteractive &&\
    apt-get install -y --no-install-recommends apt-utils openjdk-21-jre tzdata locales xfonts-utils fontconfig libreoffice-nogui pandoc python3-pip &&\
    echo 'Asia/Shanghai' > /etc/timezone &&\
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime &&\
    localedef -i zh_CN -c -f UTF-8 -A /usr/share/locale/locale.alias zh_CN.UTF-8 &&\
    locale-gen zh_CN.UTF-8 &&\
    apt-get install -y --no-install-recommends ttf-mscorefonts-installer &&\
    apt-get install -y --no-install-recommends ttf-wqy-microhei ttf-wqy-zenhei xfonts-wqy &&\
    apt-get autoremove -y &&\
    apt-get clean &&\
    rm -rf /var/lib/apt/lists/*

# 更新apt源并安装Python3和pip
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    apt-get install -y ansible && \
    apt-get install -y tree && \
    apt-get install -y git && \
    apt-get install -y vim && \
    apt-get install -y net-tools && \
    apt-get install -y iputils-ping && \
    apt-get install -y curl && \
    apt-get install -y wget && \
    apt-get install -y lsof && \
    apt-get install -y python3-apt && \
    apt-get install -y bash && \
    # 清理缓存
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 将当前目录下的requirements.txt复制到镜像中
COPY requirements.txt /app/requirements.txt

# 使用阿里云的PyPI镜像源安装requirements.txt中指定的所有Python库
RUN pip3 install  --break-system-packages --no-cache-dir -r /app/requirements.txt -i https://mirrors.aliyun.com/pypi/simple/

# 内置一些常用的中文字体，避免普遍性乱码
RUN mkdir /usr/share/fonts/chinese

RUN cd /usr/share/fonts/chinese &&\
    # 安装字体
    mkfontscale &&\
    mkfontdir &&\
    fc-cache -fv

ENV LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8