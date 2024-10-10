FROM registry.cn-shenzhen.aliyuncs.com/alinesno-infra-base/openjdk:17-slim

# 设置为中国国内源
RUN sed -i 's/deb.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list
RUN sed -i 's/security.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list

# 更新apt源并安装Python3和pip
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    apt-get install -y ansible && \
    # 清理缓存
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 将当前目录下的requirements.txt复制到镜像中
COPY requirements.txt /app/requirements.txt

# 使用阿里云的PyPI镜像源安装requirements.txt中指定的所有Python库
RUN pip3 install --no-cache-dir -r /app/requirements.txt -i https://mirrors.aliyun.com/pypi/simple/

# 如果有其他的应用代码或者文件，也可以在这里复制进去
# COPY . /app

# 设置工作目录（如果需要）
# WORKDIR /app

# 其他指令...