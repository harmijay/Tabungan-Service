package com.bank.tabungan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
public class TabunganService {
    private final TabunganRepository tabunganRepository;
    private Status status;
    Logger logger = LoggerFactory.getLogger(TabunganService.class);

    @Autowired
    public TabunganService(TabunganRepository tabunganRepository, Status status) {
        this.tabunganRepository = tabunganRepository;
        this.status = status;
    }

    public List<Tabungan> getTabungan() {
        return tabunganRepository.findAll();
    }

    public void addTabungan(Tabungan tabungan) {
        tabunganRepository.save(tabungan);
    }

    public void deleteTabungan(Integer nomorRekening) {
        boolean exists = tabunganRepository.existsById(nomorRekening);
        if(!exists) {
            throw new IllegalStateException("Tidak terdapat tabungan dengan nomor nasabah: " + nomorRekening);
        }
        tabunganRepository.deleteById(nomorRekening);
    }

    @Transactional
    public void updateTabungan(Integer nomorRekening, Long jumlah, String jenis) {
        Tabungan tabungan = tabunganRepository.findById(nomorRekening)
                .orElseThrow(() -> new IllegalStateException("Tidak terdapat tabungan dengan nomor nasabah: " + nomorRekening));

        if(jumlah != null) {
            tabungan.setSaldo(jumlah);
        }

        if(jenis != null && jenis.length() > 0) {
            tabungan.setJenisTabungan(jenis);
        }
    }

    @Transactional
    public HashMap<String, Object> tarikUang(Integer nomorRekening, Long jumlah) {
        HashMap<String, Object> response = kurangiSaldo(nomorRekening, jumlah);
        if(response.get("status").equals(status.CODE_SUCCESS)) {
            response.put("message", status.getMessageSuccessTarik(jumlah));
        }
        return response;
    }

    @Transactional
    public HashMap<String, Object> kurangiSaldo(Integer nomorRekening, Long jumlah) {
        Tabungan tabungan = tabunganRepository.findById(nomorRekening)
                .orElseThrow(() -> new IllegalStateException("Tidak terdapat tabungan dengan nomor rekening: " + nomorRekening));

        HashMap<String, Object> response = new HashMap<>();

        if(tabungan == null) {
            response.put("status", status.CODE_REKENING_NOT_FOUND);
            response.put("message", status.getMessageRekeningNotFound(nomorRekening));
        } else if(jumlah > tabungan.getSaldo()) {
            response.put("status", status.CODE_SALDO_KURANG);
            response.put("message", status.MSG_SALDO_KURANG);
        } else {
            tabungan.setSaldo(tabungan.getSaldo() - jumlah);
            response.put("status", status.CODE_SUCCESS);
            response.put("message", status.getMessageSuccessTarik(jumlah));
        }
        return response;
    }

    @Transactional
    public HashMap<String, Object> tabung(Integer nomorRekening, Long jumlah) {
        HashMap<String, Object> response = tambahSaldo(nomorRekening, jumlah);
        if(response.get("status").equals(status.CODE_SUCCESS)) {
            response.put("message", status.getMessageSuccessTabung(jumlah));
        }
        return response;
    }

    @Transactional
    public HashMap<String, Object> tambahSaldo(Integer nomorRekening, Long jumlah) {
        Tabungan tabungan = tabunganRepository.findById(nomorRekening)
                .orElseThrow(() -> new IllegalStateException("Tidak terdapat tabungan dengan nomor rekening: " + nomorRekening));

        HashMap<String, Object> response = new HashMap<>();

        if(tabungan == null) {
            response.put("status", status.CODE_REKENING_NOT_FOUND);
            response.put("message", status.getMessageRekeningNotFound(nomorRekening));
        } else {
            tabungan.setSaldo(tabungan.getSaldo() + jumlah);
            response.put("status", status.CODE_SUCCESS);
            response.put("message", status.getMessageSuccessTabung(jumlah));
        }
        return response;
    }

    @Transactional
    public HashMap<String, Object> transfer(Integer pengirim, Integer penerima, Long jumlah) {
        Tabungan tabunganPengirim = tabunganRepository.findById(pengirim)
                .orElseThrow(() -> new IllegalStateException("Tidak terdapat tabungan dengan nomor rekening: " + pengirim));
        Tabungan tabunganPenerima = tabunganRepository.findById(penerima)
                .orElseThrow(() -> new IllegalStateException("Tidak terdapat tabungan dengan nomor rekening: " + penerima));

        HashMap<String, Object> response = new HashMap<>();

        if(tabunganPengirim.getSaldo() >= jumlah && jumlah > 0) {
            tambahSaldo(tabunganPenerima.getNomorRekening(), jumlah);
            kurangiSaldo(tabunganPengirim.getNomorRekening(), jumlah);
            response.put("status", status.CODE_SUCCESS);
            response.put("message", status.getMessageSuccessTransfer(pengirim, penerima, jumlah));
        } else if(jumlah < 0) {
            response.put("status", 433);
            response.put("message", "Jumlah transfer tidak bisa negatif");
            logger.info("Jumlah transfer tidak bisa negatif");
        } else {
            response.put("status", status.CODE_SALDO_KURANG);
            response.put("message", status.MSG_SALDO_KURANG);
        }
        return response;
    }

    public HashMap<String, Object> transferFailed(HashMap<String, Object> response, String akun, Integer statusCode) {
        response.put("status", statusCode);
        if(statusCode == status.CODE_AKUN_TIDAK_TERDAFTAR) {
            response.put("message", "Akun " + akun + " tidak valid");
        } else if (statusCode == status.CODE_AKUN_DIBLOKIR) {
            response.put("message", "Akun " + akun + " diblokir");
        }
        return response;
    }

    public Object getTabunganById(Integer nomorRekening) {
        return tabunganRepository.findById(nomorRekening)
                .orElseThrow(() -> new IllegalStateException("Tidak terdapat tabungan dengan nomor rekening: " + nomorRekening));
    }
}
