<template>
  <div>
    <el-card>
      <div slot="header">
        <span>人员管理</span>
        <el-button type="primary" size="small" style="float: right" @click="openAddDialog">添加人员</el-button>
      </div>

      <el-table :data="userList" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="180" />
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button size="mini" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空则不修改密码' : '请输入初始密码'" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getUserList, addUser, updateUser, deleteUser } from '@/api/user'

export default {
  name: 'UserList',
  data() {
    return {
      userList: [],
      dialogVisible: false,
      dialogTitle: '添加人员',
      isEdit: false,
      form: { name: '', phone: '', email: '', address: '', password: '' }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      const res = await getUserList()
      this.userList = res.data.data || []
    },
    openAddDialog() {
      this.dialogTitle = '添加人员'
      this.isEdit = false
      this.form = { name: '', phone: '', email: '', address: '', password: '' }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.dialogTitle = '编辑人员'
      this.isEdit = true
      this.form = { id: row.id, name: row.name, phone: row.phone, email: row.email, address: row.address, password: '' }
      this.dialogVisible = true
    },
    async handleSave() {
      try {
        if (this.isEdit) {
          await updateUser(this.form)
        } else {
          await addUser(this.form)
        }
        this.$message.success(this.isEdit ? '更新成功' : '添加成功')
        this.dialogVisible = false
        this.loadData()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async handleDelete(id) {
      try {
        await this.$confirm('确定删除该人员吗？', '提示', { type: 'warning' })
        await deleteUser(id)
        this.$message.success('删除成功')
        this.loadData()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败')
      }
    }
  }
}
</script>
