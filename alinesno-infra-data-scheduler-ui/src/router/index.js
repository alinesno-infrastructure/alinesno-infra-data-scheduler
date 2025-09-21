import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout/SaaSLayout'
// import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
   {
     path: '/sso/login',
     component: () => import('@/views/loginSso'),
     hidden: true
   },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '/demo',
    component: () => import('@/views/demo'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    hidden: true,
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: '/index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      },

      // {
      //   path: '/dashboard/smartService',
      //   component: () => import('@/views/smartService'),
      //   name: '/dashboard/smartService',
      //   meta: { title: '智能客服', icon: 'dashboard', affix: true }
      // },
      // {
      //   path: '/dashboard/serviceList',
      //   component: () => import('@/views/serviceList'),
      //   name: '/dashboard/serviceList',
      //   meta: { title: '服务列表', icon: 'dashboard', affix: true }
      // },
      {
        path: '/dashboard/suportTechnique',
        component: () => import('@/views/suportTechnique'),
        name: '/dashboard/suportTechnique',
        meta: { title: '支持管理', icon: 'dashboard', affix: true }
      },
      {
        path: '/dashboard/learnPanel',
        component: () => import('@/views/learnPanel'),
        name: '/dashboard/learnPanel',
        meta: { title: '学习手册', icon: 'dashboard', affix: true }
      },

      // ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
      // {
      //   path: '/data/scheduler/project/index',
      //   name: 'data.scheduler.project.index',
      //   hidden: true,
      //   component: () => import('@/views/data/scheduler/project/index'),
      //   meta: { title: '项目管理', icon: 'fa-solid fa-feather', affix: true }
      // },
      {
        path: '/data/scheduler/processDefinition/index',
        name: 'data.scheduler.processDefinition.index',
        hidden: false, // 如果这个页面需要在侧边栏显示，请设置为 false
        component: () => import('@/views/data/scheduler/processDefinition/index'),
        meta: { title: '流程管理', icon: 'fa-solid fa-masks-theater', affix: false }
      },
      {
        path: '/data/scheduler/processInstance/index',
        name: 'data.scheduler.processInstance.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/processInstance/index'),
        meta: { title: '流程实例', icon: 'fa-brands fa-wordpress', affix: false }
      },
      {
        path: '/data/scheduler/catelog/index',
        name: 'data.scheduler.catelog.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/catelog/index'),
        meta: { title: '流程分类', icon: 'fa-solid fa-file-pdf', affix: false }
      },
      {
        path: '/data/scheduler/resources/index',
        name: 'data.scheduler.resources.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/resources/index'),
        meta: { title: '资源中心', icon: 'fa-solid fa-file-pdf', affix: false }
      },
      {
        path: '/data/scheduler/datasource/index',
        name: 'data.scheduler.datasource.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/datasource/index'),
        meta: { title: '数据源管理', icon: 'fa-solid fa-computer', affix: false }
      },
      {
        path: '/data/scheduler/analyse/index',
        name: 'data.scheduler.analyse.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/analyse/index'),
        meta: { title: '监控服务', icon: 'fa-brands fa-skype', affix: false }
      },
      {
        path: '/data/scheduler/llmModel/index',
        name: 'data.scheduler.llmModel.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/llmModel/index'),
        meta: { title: '大模型配置管理', icon: 'fa-solid fa-computer', affix: false }
      },
      {
        path: '/data/scheduler/configuration/index',
        name: 'data.scheduler.configuration.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/configuration/index'),
        meta: { title: '配置管理', icon: 'fa-solid fa-computer', affix: false }
      },
      {
        path: '/data/scheduler/apiRecord/index',
        name: 'data.scheduler.apiRecord.index',
        hidden: false,
        component: () => import('@/views/data/scheduler/apiRecord/index'),
        meta: { title: '操作记录', icon: 'fa-solid fa-code-pull-request', affix: false }
      },

      // ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
      {
        path: '/data/scheduler/processDefinition/addDefinition',
        name: 'data.scheduler.processDefinition.addDefinition',
        hidden: false,
        component: () => import('@/views/data/scheduler/processDefinition/addDefinition'),
        meta: { title: '添加任务', icon: 'fa-solid fa-code-pull-request', affix: false }
      },
      {
        path: '/data/scheduler/processDefinition/createDefinition',
        name: 'data.scheduler.processDefinition.createDefinition',
        hidden: false,
        component: () => import('@/views/data/scheduler/processDefinition/createDefinition'),
        meta: { title: '添加任务', icon: 'fa-solid fa-code-pull-request', affix: false }
      },
      {
        path: '/data/scheduler/datasource/addSource',
        name: 'data.scheduler.datasource.addSource',
        hidden: true,
        component: () => import('@/views/data/scheduler/datasource/addSource'),
        meta: { title: '创建读取源', icon: 'dashboard', affix: true }
      }

    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
});

export default router;
