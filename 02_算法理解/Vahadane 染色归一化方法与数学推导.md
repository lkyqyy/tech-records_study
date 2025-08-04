# 病理图像颜色归一化(Vahadane 方法)

> 编写时间：2025-08-01  
> 用途：病理颜色标准化  
> 来源论文:
>Structure-Preserving Color Normalization and Sparse Stain Separation for Histological Images  
> 链接：[IEEE文章链接](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7460968)

(出现乱码，待修正)

---

## 1. 方法简介

Vahadane 方法是一种用于数字病理图像的染色标准化技术，其目标是将不同来源或批次的图像染色风格进行对齐，从而增强模型在多中心病理图像上的泛化能力。该方法通过将图像转换为光密度矩阵（OD），并对其进行稀疏非负矩阵分解（SNMF）分别提取染料颜色基和浓度信息，最后使用参考图像的颜色基和源图的染料浓度重构 OD 光密度矩阵，再转回 RGB图像，从而完成颜色标准化。

**核心流程**:

1. 将待归一化图像转为光密度（OD）矩阵，并使用稀疏非负矩阵分解（SNMF）得到染料颜色基$W$和染料浓度$H$；
2. 对参考图像执行同样的 SNMF 分解，得到其染料颜色基 $W_ref$ 和浓度矩阵 $H_ref$；
3. 迭代优化 $W$ 和 $H$，直到收敛；
4. 用 $W_ref$ 和源图 $H$ ，重构 OD，再转回 RGB，即得标准化图像；

---

## 2. 相关背景知识

**光密度矩阵 OD 转换原理**: 根据比尔–朗伯定律：

$$
OD = -\log_{10} (I / I_0)
$$

$I$ 为 RGB 值 (0–255);  $I_0$ 为最大光强(255)

关于OD的理解：

* OD 表示光的吸收度；
* OD 空间里，颜色吸光度和浓度可线性分解
* 颜色深 → OD 值高，吸光多；颜色淡 → OD 值低，吸光少

**染料色基 W 解释**: W 是 3x2 的矩阵：

$$
W = \begin{bmatrix} r_H & r_E \\ g_H & g_E \\ b_H & b_E \end{bmatrix}
$$

* H: Hematoxylin (苏木素)
* E: Eosin (伊红)
* 每列表示两种不同的颜色, 而 RGB 三行应对三个通道的光吸收度

**染料浓度矩阵 H**: 每一列表示第 $i$ 个像素的染料混合比例

$$
H = \begin{bmatrix}
h_{11} & h_{12} & \cdots & h_{1n} \\
h_{21} & h_{22} & \cdots & h_{2n}
\end{bmatrix}
$$

$h_{1i}$：像素 $i$ 中 **苏木素（Hematoxylin）** 的浓度  
$h_{2i}$：像素 $i$ 中 **伊红（Eosin）** 的浓度

---

## 3. SNMF 模型分解求解

由于OD矩阵中的像素的颜色吸光度和浓度可线性分解（我的理解是浓度越大，颜色越深）, 于是该矩阵可被分解为:
$$OD \approx W \cdot H$$

因为$OD = WH$ 解的空间是无限的，有无数个解。所以，为了“找出最合理的参数组合”，我们引入一个目标函数，通过最小化重建误差来寻找最优解, 目标函数:

$$\min_{W,H} \|OD - WH\|_F^2 + \lambda \|H\|_1$$

交替优化，求解最优的 W 矩阵与 H 矩阵:

### 3.1 固定 W 参数矩阵，求解 H 参数矩阵

目标函数:
$$\min_H \|OD - WH\|_F^2 + \lambda \|H\|_1$$
那么根据向量L2范数平方等于自身转置乘自身，那么目标函数可以修改为:【以OD矩阵中第一个像素为例】

$$L(\mathbf{h}_1) = \left\| \mathbf{o}_1 - \mathbf{W} \mathbf{h}_1 \right\|_2^2 = (\mathbf{o}_1 - \mathbf{W} \mathbf{h}_1)^T (\mathbf{o}_1 - \mathbf{W} \mathbf{h}_1)$$

其中：

- $\mathbf{o}_1 \in \mathbb{R}^{3 \times 1}$：列向量，表示第 1 个像素的光密度（OD）
- $\mathbf{W} \in \mathbb{R}^{3 \times 2}$：染料颜色基矩阵，每列是一个染料的吸光特征向量
- $\mathbf{h}_1 \in \mathbb{R}^{2 \times 1}$：待优化的染料浓度向量

根据向量内积展开

$$
(\mathbf{o}_1 - \mathbf{W}\mathbf{h}_1)^\top (\mathbf{o}_1 - \mathbf{W}\mathbf{h}_1)
= \mathbf{o}_1^\top \mathbf{o}_1 - \mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 - (\mathbf{W} \mathbf{h}_1)^\top \mathbf{o}_1 + (\mathbf{W} \mathbf{h}_1)^\top (\mathbf{W} \mathbf{h}_1)
$$

通过化简，

$$
\begin{aligned}
&(\mathbf{o}_1 - \mathbf{W} \mathbf{h}_1)^\top (\mathbf{o}_1 - \mathbf{W} \mathbf{h}_1) \\
&= \mathbf{o}_1^\top \mathbf{o}_1 - \mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 - (\mathbf{W} \mathbf{h}_1)^\top \mathbf{o}_1 + (\mathbf{W} \mathbf{h}_1)^\top (\mathbf{W} \mathbf{h}_1) \\
&= \mathbf{o}_1^\top \mathbf{o}_1 - 2 \mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 + \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1 \\
&= \mathbf{o}_1^\top \mathbf{o}_1 - 2 \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{o}_1 + \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
\end{aligned}
$$

观察中间两项：

- $\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1$ 是一个标量；
- $(\mathbf{W} \mathbf{h}_1)^\top \mathbf{o}_1$ 是它的转置，但标量的转置等于它本身！

所以这两项是相等的：

$$
\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 = (\mathbf{W} \mathbf{h}_1)^\top \mathbf{o}_1
$$

于是可以合并为：

$$
-\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 - (\mathbf{W} \mathbf{h}_1)^\top \mathbf{o}_1 = -2 \mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1
$$

进一步地，我们可以将 $\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1$ 变换为：

$$
\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1
= (\mathbf{W}^\top \mathbf{o}_1)^\top \mathbf{h}_1
= \mathbf{h}_1^\top (\mathbf{W}^\top \mathbf{o}_1)
$$

因此，有：

$$
\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 = \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{o}_1
$$

所以，最终合并项可写为：

$$
-2 \mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1 = -2 \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{o}_1
$$


最后一项：

$$
(\mathbf{W} \mathbf{h}_1)^\top (\mathbf{W} \mathbf{h}_1)
$$

是一个矩阵乘积（标量结果），我们可以将其重写为：

$$
= \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
$$

原因是：

$$
(\mathbf{W} \mathbf{h}_1)^\top = \mathbf{h}_1^\top \mathbf{W}^\top
$$

因此：

$$
(\mathbf{W} \mathbf{h}_1)^\top (\mathbf{W} \mathbf{h}_1) = \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
$$

因此，目标函数可以写成：

$$
L(\mathbf{h}_1) = \mathbf{o}_1^\top \mathbf{o}_1 - 2 \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{o}_1 + \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
$$

现在是对该目标函数求导：因为第一项是常数项，导数为0，根据向量的导数公式：

$$
\nabla_{\mathbf{h}} \left( \mathbf{b}^\top \mathbf{h} \right) = \mathbf{b}
$$
可以得到第二项的求导结果：
$$
\nabla_{\mathbf{h}_1} \left( -2 \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{o}_1 \right) = -2 \mathbf{W}^\top \mathbf{o}_1
$$

我们考虑项：

$$
\mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
$$

这是一个标准的二次型形式，设对称矩阵 $\mathbf{A}$，有公式：

$$
\nabla_{\mathbf{h}} \left( \mathbf{h}^\top \mathbf{A} \mathbf{h} \right) = 2 \mathbf{A} \mathbf{h} \quad \text{（若 } \mathbf{A} = \mathbf{A}^\top \text{）}
$$

在这里我们令：

$$
\mathbf{A} = \mathbf{W}^\top \mathbf{W}
$$

由于 $\mathbf{A}$ 是矩阵与其自身转置的乘积，$A^\top A$ 与 $A A^\top$ 必为对称矩阵，因此是对称的：

$$
(\mathbf{W}^\top \mathbf{W})^\top = \mathbf{W}^\top \mathbf{W}
$$

所以我们可得梯度：

$$
\nabla_{\mathbf{h}_1} \left( \mathbf{h}_1^\top \mathbf{W}^\top \mathbf{W} \mathbf{h}_1 \right)
= 2 \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
$$

因此，最终的梯度为：

$$
\nabla_{h_1} L(\mathbf{h}_1) = -2 \mathbf{W}^\top \mathbf{o}_1 + 2 \mathbf{W}^\top \mathbf{W} \mathbf{h}_1
$$

梯度更新规则为：

$$
\mathbf{h}_1 = \mathbf{h}_1 - \eta \nabla_{h_1} L(\mathbf{h}_1)
$$

其中，$\eta$ 是学习率。这就是稀疏非负矩阵分解的全过程。，通过不断迭代优化，计算最佳H矩阵。

> 注：虽然项 $\mathbf{o}_1^\top \mathbf{W} \mathbf{h}_1$ 是一个标量（即一个数），但它是一个关于 $\mathbf{h}_1$ 的表达式。因此，它不能被视为常数，它对 $\mathbf{h}_1$ 的导数 **不为 0**；

---

### 3.2 固定 H 参数矩阵，求解 W 参数矩阵

目标函数的化简与上面相同，仅对W求导有所不同；所以这里仅对W矩阵求导进行推导：同样第一项与W无关，导数为0，
对于第二项：
$$-2 \mathbf{o}_1^\top W \mathbf{h}_1$$

我们使用双线性形式导数恒等式：

若 $a \in \mathbb{R}^m$，$b \in \mathbb{R}^r$，$W \in \mathbb{R}^{m \times r}$，则

$$
\nabla_W (a^\top W b) = a b^\top
$$

因此：

$$
\nabla_W \left( -2 \mathbf{o}_1^\top W \mathbf{h}_1 \right) = -2 \mathbf{o}_1 \mathbf{h}_1^\top
$$

而第三项：

$$\mathbf{h}_1^\top W^\top W \mathbf{h}_1$$

是一个关于 $W$ 的二次型项。我们利用如下恒等式：

若 $x \in \mathbb{R}^r$, $W \in \mathbb{R}^{m \times r}$，则  

$$
\nabla_W \left( \|W x\|_2^2 \right) = 2 W x x^\top
$$

于是有：

$$
\nabla_W \left( \mathbf{h}_1^\top W^\top W \mathbf{h}_1 \right) = 2 W \mathbf{h}_1 \mathbf{h}_1^\top
$$

最终梯度表达式（单样本）

三项相加得：

$$
\nabla_W L(W) = 2 (W \mathbf{h}_1 - \mathbf{o}_1) \mathbf{h}_1^\top
$$

给定学习率（步长）$\eta > 0$，梯度下降法的标准更新规则为：

$$
W^{(t+1)} = W^{(t)} - \eta \cdot \nabla_W L\left(W^{(t)}\right)
$$

---

## 4. 根据新染色风格重建 OD 并转移到 RGB

已知条件

$H_{\text{source}} \in \mathbb{R}^{2 \times n}$：迭代优化后的源图像染料浓度矩阵（$n$ 为像素总数）；
$W_{\text{ref}} \in \mathbb{R}^{3 \times 2}$：参考图像的颜色基矩阵，表示标准染色风格；
$OD_{\text{new}} = W_{\text{ref}} \cdot H_{\text{source}} \in \mathbb{R}^{3 \times n}$：新的光密度矩阵；
转换公式： 

  $$
  RGB = 255 \cdot \exp(-OD)
  $$

公式推导: 根据参考染色风格重建 OD

$$
OD_{\text{normalized}} = W_{\text{ref}} \cdot H_{\text{source}}
$$

其中：

$W_{\text{ref}}$：每列表示参考图像中染料的吸光方向（即颜色基）；
$H_{\text{source}}$：表示每个像素在两种染料下的浓度；

从 OD 空间还原 RGB 图像,根据光密度定义公式：

$$
OD = -\log_{10} \left( \frac{I}{I_0} \right) \quad \Rightarrow \quad I = I_0 \cdot 10^{-OD}
$$

实际使用中，多采用自然对数方式还原，变为指数形式：

$$
RGB = 255 \cdot \exp(-OD_{\text{normalized}})
$$

---

## 5.手工模拟计算示例

输入光密度矩阵 OD ∈ ℝ<sup>3×4</sup>：

每列是一个像素的 RGB 光密度：

$$
OD =
\begin{bmatrix}
0.3 & 0.5 & 0.2 & 0.4 \\
0.6 & 0.7 & 0.3 & 0.5 \\
0.1 & 0.3 & 0.1 & 0.2
\end{bmatrix}
$$

初始值设定：
染料颜色基矩阵 $W \in \mathbb{R}^{3 \times 2}$：

$$
W^{(0)} =
\begin{bmatrix}
0.6 & 0.2 \\
0.5 & 0.3 \\
0.4 & 0.1
\end{bmatrix}
$$

染料浓度矩阵 $H^{(0)} \in \mathbb{R}^{2 \times 4}$：

$$
H^{(0)} =
\begin{bmatrix}
0.4 & 0.6 & 0.3 & 0.5 \\
0.2 & 0.1 & 0.2 & 0.1
\end{bmatrix}
$$

---

选择第一个像素进行重建

原始 OD 向量：

$$
\mathbf{o}_1 = \begin{bmatrix} 0.3 \\ 0.6 \\ 0.1 \end{bmatrix}
\quad , \quad
\mathbf{h}_1 = \begin{bmatrix} 0.4 \\ 0.2 \end{bmatrix}
$$

$$
\hat{\mathbf{o}}_1 = W \cdot \mathbf{h}_1
=
\begin{bmatrix}
0.6 \cdot 0.4 + 0.2 \cdot 0.2 \\
0.5 \cdot 0.4 + 0.3 \cdot 0.2 \\
0.4 \cdot 0.4 + 0.1 \cdot 0.2
\end{bmatrix}
=
\begin{bmatrix}
0.28 \\
0.26 \\
0.18
\end{bmatrix}
$$

残差：

$$
\mathbf{r} = \mathbf{o}_1 - \hat{\mathbf{o}}_1
=
\begin{bmatrix}
0.3 - 0.28 \\
0.6 - 0.26 \\
0.1 - 0.18
\end{bmatrix}
=
\begin{bmatrix}
0.02 \\
0.34 \\
-0.08
\end{bmatrix}
$$

计算梯度

$$
\nabla_{\mathbf{h}_1} = -2 W^\top \mathbf{r}
=
-2 \cdot
\begin{bmatrix}
0.6 & 0.5 & 0.4 \\
0.2 & 0.3 & 0.1
\end{bmatrix}
\cdot
\begin{bmatrix}
0.02 \\
0.34 \\
-0.08
\end{bmatrix}
=
-2 \cdot
\begin{bmatrix}
0.3 \\
0.196
\end{bmatrix}
=
\begin{bmatrix}
-0.6 \\
-0.392
\end{bmatrix}
$$

使用学习率更新浓度向量（η = 0.1）

$$
\mathbf{h}_1^{\text{new}} = \mathbf{h}_1 - \eta \nabla_{\mathbf{h}_1}
=
\begin{bmatrix}
0.4 \\
0.2
\end{bmatrix}
+ 0.1 \cdot
\begin{bmatrix}
0.6 \\
0.392
\end{bmatrix}
=
\begin{bmatrix}
0.46 \\
0.2392
\end{bmatrix}
$$

类似步骤可批量处理所有列（像素），完成整个 $H$ 的更新。

> 不需要非负约束修正，直接进入下一轮迭代。如果出现负数，可以在更新后检查是否全为0，如果是，则将所有元素设置为一个很小的正数，例如 1e-8。

---

固定 H，更新 W：

输入：

$$
OD = \begin{bmatrix}
0.3 & 0.5 & 0.2 & 0.4 \\
0.6 & 0.7 & 0.3 & 0.5 \\
0.1 & 0.3 & 0.1 & 0.2
\end{bmatrix}, \quad
W = \begin{bmatrix}
0.6 & 0.2 \\
0.5 & 0.3 \\
0.4 & 0.1
\end{bmatrix}, \quad
H = \begin{bmatrix}
0.46 & 0.6 & 0.3 & 0.5 \\
0.2392 & 0.1 & 0.2 & 0.1
\end{bmatrix}
$$

计算 WH

矩阵乘法：

$$
WH = W \cdot H =
\begin{bmatrix}
0.34192 & 0.38 & 0.22 & 0.34 \\
0.35876 & 0.33 & 0.21 & 0.28 \\
0.20792 & 0.25 & 0.14 & 0.22
\end{bmatrix}
$$

计算残差 \( R = OD - WH \)

$$
R =
\begin{bmatrix}
0.3 - 0.34192 & 0.5 - 0.38 & 0.2 - 0.22 & 0.4 - 0.34 \\
0.6 - 0.35876 & 0.7 - 0.33 & 0.3 - 0.21 & 0.5 - 0.28 \\
0.1 - 0.20792 & 0.3 - 0.25 & 0.1 - 0.14 & 0.2 - 0.22
\end{bmatrix}
=
\begin{bmatrix}
-0.04192 & 0.12 & -0.02 & 0.06 \\
0.24124 & 0.37 & 0.09 & 0.22 \\
-0.10792 & 0.05 & -0.04 & -0.02
\end{bmatrix}
$$

计算梯度 \( \nabla_W = -2 R H^T \)

先写出 \( H^T \)：

$$
H^T =
\begin{bmatrix}
0.46 & 0.2392 \\
0.6 & 0.1 \\
0.3 & 0.2 \\
0.5 & 0.1
\end{bmatrix}
$$

计算第一行的梯度（仅举例）：

$$
\text{Row}_1 = [-0.04192, 0.12, -0.02, 0.06] \cdot H^T =
\begin{bmatrix}
0.0978 & 0.0157
\end{bmatrix}
$$

乘以 -2：

$$
\nabla W_{1,:} = -2 \cdot [0.0978, 0.0157] = [-0.1956, -0.0314]
$$

更新 W（学习率 \( \eta = 0.1 \)）

$$
W_{1,:}^{\text{new}} = W_{1,:} - \eta \cdot \nabla W_{1,:}
= [0.6, 0.2] + 0.1 \cdot [0.1956, 0.0314]
= [0.6196, 0.2031]
$$

对其余行亦可按相同方式计算。更新完成后可继续下一轮 W / H 的交替更新

---

使用参考颜色基重建 RGB：给定参考颜色基，

$$
W_{ref} = \begin{bmatrix}
0.5 & 0.2 \\
0.4 & 0.3 \\
0.2 & 0.1
\end{bmatrix}
$$

使用我们刚刚更新得到的染料浓度向量：

$$
h_1^{\text{new}} = \begin{bmatrix} 0.46 \\ 0.2392 \end{bmatrix}
$$

进行矩阵乘法计算新 OD：

$$
OD_{\text{new}} = W_{ref} \cdot h_1 =
\begin{bmatrix}
0.5 \cdot 0.46 + 0.2 \cdot 0.2392 = 0.23 + 0.04784 = 0.27784 \\
0.4 \cdot 0.46 + 0.3 \cdot 0.2392 = 0.184 + 0.07176 = 0.25576 \\
0.2 \cdot 0.46 + 0.1 \cdot 0.2392 = 0.092 + 0.02392 = 0.11592
\end{bmatrix}
$$

即：

$$
OD_{\text{new}} = \begin{bmatrix} 0.27784 \\ 0.25576 \\ 0.11592 \end{bmatrix}
$$

---

将 OD 转回 RGB 值

转换公式为：

$$
RGB = 255 \cdot \exp(-OD)
$$

分别计算每个通道：

$$
\begin{aligned}
& R = 255 \cdot e^{-0.27784} \approx 255 \cdot 0.7573 \approx 193 \\
& G = 255 \cdot e^{-0.25576} \approx 255 \cdot 0.7744 \approx 197 \\
& B = 255 \cdot e^{-0.11592} \approx 255 \cdot 0.8906 \approx 227
\end{aligned}
$$

---

最终重建像素 RGB：
$$
RGB = [193, \ 197, \ 227]
$$
