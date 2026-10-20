import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { addressApi } from '@/api/mall';
import { loadRegions, type RegionNode } from '@/data/regions';
import { toast } from '@/utils/feedback';
import type { Address } from '@/types/mall';

interface FormState {
  id?: string;
  receiver: string;
  mobile: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: boolean;
}

const EMPTY: FormState = {
  receiver: '',
  mobile: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
};

export default function MAddress() {
  const navigate = useNavigate();
  const [list, setList] = useState<Address[]>([]);
  const [regions, setRegions] = useState<RegionNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<FormState | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await addressApi.list();
      setList(data);
    } catch {
      /* 拦截器已提示 */
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    void loadRegions().then(setRegions).catch(() => undefined);
  }, [load]);

  const provinces = regions;
  const cities = useMemo(
    () => provinces.find((item) => item.name === form?.province)?.children ?? [],
    [provinces, form?.province],
  );
  const districts = useMemo(
    () => cities.find((item) => item.name === form?.city)?.children ?? [],
    [cities, form?.city],
  );

  const openCreate = () => setForm({ ...EMPTY, isDefault: list.length === 0 });

  const openEdit = (address: Address) => {
    setForm({
      id: String(address.id),
      receiver: address.receiver,
      mobile: address.mobile,
      province: address.province,
      city: address.city,
      district: address.district ?? '',
      detail: address.detail,
      isDefault: address.isDefault === 1,
    });
  };

  const save = async () => {
    if (!form) return;
    if (!form.receiver.trim()) return toast.warning('请填写收货人');
    if (!/^1\d{10}$/.test(form.mobile.trim())) return toast.warning('请填写正确的手机号');
    if (!form.province || !form.city) return toast.warning('请选择省市区');
    if (!form.detail.trim()) return toast.warning('请填写详细地址');

    setSaving(true);
    try {
      const data = {
        id: form.id,
        receiver: form.receiver.trim(),
        mobile: form.mobile.trim(),
        province: form.province,
        city: form.city,
        district: form.district,
        detail: form.detail.trim(),
        isDefault: form.isDefault,
      };
      if (form.id) await addressApi.update(data);
      else await addressApi.create(data);
      toast.success('已保存');
      setForm(null);
      await load();
    } catch {
      /* 拦截器已提示 */
    } finally {
      setSaving(false);
    }
  };

  const remove = async (address: Address) => {
    try {
      await addressApi.remove(String(address.id));
      toast.success('已删除');
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const setDefault = async (address: Address) => {
    try {
      await addressApi.setDefault(String(address.id));
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div style={{ paddingBottom: 84 }}>
      {loading && <div className="m-skeleton" />}

      {!loading && list.length === 0 && <div className="m-empty">还没有收货地址</div>}

      {list.map((address) => (
        <div key={address.id} className="m-address">
          <div className="m-address__main">
            <div className="m-address__who">
              <span>{address.receiver}</span>
              <span style={{ fontWeight: 400, color: '#6f675e', fontSize: 13 }}>{address.mobile}</span>
              {address.isDefault === 1 && <span className="m-tag">默认</span>}
            </div>
            <div className="m-address__text">{address.fullAddress}</div>
            <div style={{ display: 'flex', gap: 14, marginTop: 10 }}>
              {address.isDefault !== 1 && (
                <button
                  onClick={() => void setDefault(address)}
                  style={{ border: 0, background: 'none', color: '#ff5a1f', fontSize: 12, padding: 0 }}
                >
                  设为默认
                </button>
              )}
              <button
                onClick={() => openEdit(address)}
                style={{ border: 0, background: 'none', color: '#4a443c', fontSize: 12, padding: 0 }}
              >
                编辑
              </button>
              <button
                onClick={() => void remove(address)}
                style={{ border: 0, background: 'none', color: '#a89f95', fontSize: 12, padding: 0 }}
              >
                删除
              </button>
            </div>
          </div>
        </div>
      ))}

      <div className="m-detailbar">
        {list.length > 0 && (
          <button className="m-btn m-btn--block" onClick={() => navigate(-1)}>
            返回
          </button>
        )}
        <button className="m-btn m-btn--primary m-btn--block" onClick={openCreate}>
          + 新增收货地址
        </button>
      </div>

      {form && (
        <>
          <div className="m-mask" onClick={() => setForm(null)} />
          <div className="m-sheet">
            <button className="m-sheet__close" onClick={() => setForm(null)}>×</button>
            <div className="m-sheet__title">{form.id ? '编辑地址' : '新增地址'}</div>

            <div className="m-field">
              <span className="m-field__label">收货人</span>
              <input
                className="m-input"
                value={form.receiver}
                onChange={(event) => setForm({ ...form, receiver: event.target.value })}
                placeholder="姓名"
              />
            </div>
            <div className="m-field">
              <span className="m-field__label">手机号</span>
              <input
                className="m-input"
                value={form.mobile}
                inputMode="numeric"
                maxLength={11}
                onChange={(event) => setForm({ ...form, mobile: event.target.value.replace(/\D/g, '') })}
                placeholder="11 位手机号"
              />
            </div>

            <div className="m-field" style={{ alignItems: 'flex-start' }}>
              <span className="m-field__label" style={{ paddingTop: 10 }}>省市区</span>
              <div className="m-selects">
                <select
                  className="m-select"
                  value={form.province}
                  onChange={(event) =>
                    setForm({ ...form, province: event.target.value, city: '', district: '' })
                  }
                >
                  <option value="">省份</option>
                  {provinces.map((item) => (
                    <option key={item.name} value={item.name}>
                      {item.name}
                    </option>
                  ))}
                </select>
                <select
                  className="m-select"
                  value={form.city}
                  disabled={!form.province}
                  onChange={(event) => setForm({ ...form, city: event.target.value, district: '' })}
                >
                  <option value="">城市</option>
                  {cities.map((item) => (
                    <option key={item.name} value={item.name}>
                      {item.name}
                    </option>
                  ))}
                </select>
                <select
                  className="m-select"
                  value={form.district}
                  disabled={!form.city || districts.length === 0}
                  onChange={(event) => setForm({ ...form, district: event.target.value })}
                >
                  <option value="">{districts.length ? '区县' : '无'}</option>
                  {districts.map((item) => (
                    <option key={item.name} value={item.name}>
                      {item.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="m-field" style={{ alignItems: 'flex-start' }}>
              <span className="m-field__label" style={{ paddingTop: 12 }}>详细地址</span>
              <textarea
                className="m-textarea"
                value={form.detail}
                maxLength={120}
                onChange={(event) => setForm({ ...form, detail: event.target.value })}
                placeholder="街道、门牌号、楼层等"
              />
            </div>

            <div className="m-field">
              <span className="m-field__label">设为默认</span>
              <button
                className={`m-switch${form.isDefault ? ' is-on' : ''}`}
                style={{ marginLeft: 'auto' }}
                onClick={() => setForm({ ...form, isDefault: !form.isDefault })}
                aria-label="设为默认地址"
              />
            </div>

            <button
              className="m-btn m-btn--primary m-btn--block"
              style={{ marginTop: 16 }}
              disabled={saving}
              onClick={() => void save()}
            >
              保存
            </button>
          </div>
        </>
      )}
    </div>
  );
}
